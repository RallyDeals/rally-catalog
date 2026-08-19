package com.rally.catalog.client;

import com.rally.catalog.client.dto.DealActiveSummary;

import java.util.List;

public interface DealServiceClient {
    boolean hasActiveDeal(String productId);
    List<DealActiveSummary> getActiveDealsForProduct(String productId);
}
