package com.rally.catalog.client.impl;

import com.rally.catalog.client.DealServiceClient;
import com.rally.catalog.client.dto.DealActiveResponse;
import com.rally.catalog.client.dto.DealActiveSummary;
import com.rally.common.exceptions.shared.InternalServerErrorException;
import com.rally.common.exceptions.shared.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Profile("prod")
@Component
@RequiredArgsConstructor
public class DealServiceClientImpl implements DealServiceClient {

    private final RestTemplate restTemplate;

    @Value("${deal.service.url}")
    private String dealServiceUrl;

    @Override
    public boolean hasActiveDeal(String productId) {
        String url = dealServiceUrl + "/internal/deals/product/{productId}/has-active-deals";

        try {
            ResponseEntity<DealActiveResponse> response = restTemplate.getForEntity(
                    url, DealActiveResponse.class, productId);
            return response.getBody() != null && response.getBody().hasActiveDeal();
        } catch (ResourceAccessException resourceAccessException) {
            throw new ServiceUnavailableException("Deal service is unavailable");
        } catch (HttpServerErrorException serverErrorException) {
            throw new InternalServerErrorException("Deal service returned server error");
        }
    }

    @Override
    public List<DealActiveSummary> getActiveDealsForProduct(String productId) {
        String url = dealServiceUrl + "/deals?productId={productId}&status=ACTIVE,PENDING";

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {},
                    productId);

            if (response.getBody() == null || !response.getBody().containsKey("content")) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
            if (content == null) return Collections.emptyList();

            return content.stream().map(this::mapToSummary).toList();
        } catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("Deal service is unavailable");
        } catch (HttpServerErrorException e) {
            throw new InternalServerErrorException("Deal service returned server error");
        }
    }

    private DealActiveSummary mapToSummary(Map<String, Object> map) {
        DealActiveSummary summary = new DealActiveSummary();
        summary.setDealId(map.get("id") != null ? map.get("id").toString() : null);
        summary.setDealPrice(map.get("dealPrice") != null
                ? new java.math.BigDecimal(map.get("dealPrice").toString()) : null);
        summary.setDealStock(map.get("dealStock") != null ? (Integer) map.get("dealStock") : 0);
        summary.setCurrentParticipants(map.get("currentParticipants") != null
                ? (Integer) map.get("currentParticipants") : 0);
        summary.setMinParticipants(map.get("minParticipants") != null
                ? (Integer) map.get("minParticipants") : 0);
        summary.setStatus(map.get("status") != null ? map.get("status").toString() : null);
        if (map.get("durationMinutes") != null) {
            summary.setDurationMinutes((Integer) map.get("durationMinutes"));
        }
        if (map.get("endTime") != null) {
            summary.setEndTime(java.time.OffsetDateTime.parse(map.get("endTime").toString()));
        }
        return summary;
    }
}
