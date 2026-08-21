package com.rally.catalog.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DealProductResponse {
    private String id;
    private String sellerId;
    private String sellerName;
    private String productName;
    private CategoryResponse category;
    private String sku;
    private String productImageUrl;
}
