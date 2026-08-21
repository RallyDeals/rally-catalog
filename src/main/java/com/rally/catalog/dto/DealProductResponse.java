package com.rally.catalog.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
    private List<String> productImages;
}
