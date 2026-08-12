package com.rally.catalog.client.impl;

import com.rally.catalog.client.DealServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class DealServiceFakeClientImpl implements DealServiceClient {

    @Value("${deal.service.mock.has-active-deal:false}")
    private boolean hasActiveDeal;

    @Override
    public boolean hasActiveDeal(String productId) {
        return hasActiveDeal;
    }
}
