package com.rally.catalog.dto;

import com.rally.catalog.entity.ProductStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ProductResponse {

    private UUID id;
    private UUID sellerId;
    private String sellerName;
    private String name;
    private String description;
    private CategoryResponse category;
    private BigDecimal basePrice;
    private String sku;
    private boolean visible;
    private List<String> tags;
    private String imageUrl;
    private List<String> images;
    private ProductStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
    private LocalDateTime deletedAt;
}
