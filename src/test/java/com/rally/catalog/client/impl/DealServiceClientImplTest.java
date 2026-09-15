package com.rally.catalog.client.impl;

import com.rally.catalog.client.DealServiceClient;
import com.rally.common.exceptions.shared.InternalServerErrorException;
import com.rally.common.exceptions.shared.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DealServiceClientImplTest {

    private static final String DEAL_SERVICE_URL = "http://deal-service:8085";
    private static final String PRODUCT_ID = "11111111-1111-1111-1111-111111111111";
    private static final String ENDPOINT = DEAL_SERVICE_URL
            + "/internal/deals/product/" + PRODUCT_ID + "/has-active-deals";

    private MockRestServiceServer mockServer;
    private DealServiceClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        DealServiceClientImpl impl = new DealServiceClientImpl(restTemplate);
        ReflectionTestUtils.setField(impl, "dealServiceUrl", DEAL_SERVICE_URL);
        client = impl;
    }

    @Test
    void trueWhenDealServiceReportsActiveDeal() {
        mockServer.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(
                        "{\"productId\":\"" + PRODUCT_ID + "\",\"hasActiveDeals\":true}",
                        MediaType.APPLICATION_JSON));

        assertTrue(client.hasActiveDeal(PRODUCT_ID));
        mockServer.verify();
    }

    @Test
    void falseWhenDealServiceReportsNoActiveDeal() {
        mockServer.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(
                        "{\"productId\":\"" + PRODUCT_ID + "\",\"hasActiveDeals\":false}",
                        MediaType.APPLICATION_JSON));

        assertFalse(client.hasActiveDeal(PRODUCT_ID));
        mockServer.verify();
    }

    @Test
    void emptyBodyBehavesAsNoActiveDeal() {
        mockServer.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertFalse(client.hasActiveDeal(PRODUCT_ID));
        mockServer.verify();
    }

    @Test
    void serverErrorMapsToInternalServerError() {
        mockServer.expect(requestTo(ENDPOINT)).andRespond(withServerError());

        assertThrows(InternalServerErrorException.class,
                () -> client.hasActiveDeal(PRODUCT_ID));
        mockServer.verify();
    }

    @Test
    void connectionFailureMapsToServiceUnavailable() {
        mockServer.expect(requestTo(ENDPOINT))
                .andRespond(withException(new IOException("deal service down")));

        assertThrows(ServiceUnavailableException.class,
                () -> client.hasActiveDeal(PRODUCT_ID));
        mockServer.verify();
    }
}