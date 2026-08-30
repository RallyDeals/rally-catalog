package com.rally.catalog.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class DealProductResponse {
    private UUID id;
    private UUID sellerId;
    private String sellerName;
    private String productName;
    private CategoryResponse category;
    private String sku;
    private String productImageUrl;
    private List<String> productImages;
}
