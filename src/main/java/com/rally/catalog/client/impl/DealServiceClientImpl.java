package com.rally.catalog.client.impl;

import com.rally.catalog.client.DealServiceClient;
import com.rally.catalog.client.dto.DealActiveResponse;
import com.rally.common.exceptions.shared.InternalServerErrorException;
import com.rally.common.exceptions.shared.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Profile("prod")
@Component
@RequiredArgsConstructor
public class DealServiceClientImpl implements DealServiceClient {

    private final RestTemplate restTemplate;

    @Value("${deal.service.url}")
    private String dealServiceUrl;

    @Override
    public boolean hasActiveDeal(String productId) {
        String url = dealServiceUrl + "/internal/deals?productId={productId}&active=true";

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
}
