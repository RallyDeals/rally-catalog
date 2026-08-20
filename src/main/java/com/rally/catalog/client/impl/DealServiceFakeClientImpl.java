package com.rally.catalog.client.impl;

import com.rally.catalog.client.DealServiceClient;
import com.rally.catalog.client.dto.DealActiveSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Profile("stub")
public class DealServiceFakeClientImpl implements DealServiceClient {

    @Value("${deal.service.mock.has-active-deal:false}")
    private boolean hasActiveDeal;

    @Override
    public boolean hasActiveDeal(String productId) {
        return hasActiveDeal;
    }

    @Override
    public List<DealActiveSummary> getActiveDealsForProduct(String productId) {
        return Collections.emptyList();
    }
}
