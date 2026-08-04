package com.rally.catalog.dto;

import com.rally.catalog.entity.Product;
import com.rally.catalog.entity.ProductStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ProductResponse {

    private String id;
    private String sellerId;
    private String name;
    private String description;
    private CategoryResponse category;
    private BigDecimal basePrice;
    private String imageUrl;
    private ProductStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse from(Product product) {
        ProductResponse res = new ProductResponse();
        res.id = product.getId();
        res.sellerId = product.getSellerId();
        res.name = product.getName();
        res.description = product.getDescription();
        res.category = product.getCategory() == null ? null : CategoryResponse.from(product.getCategory());
        res.basePrice = product.getBasePrice();
        res.imageUrl = product.getImageUrl();
        res.status = product.getStatus();
        res.rejectionReason = product.getRejectionReason();
        res.createdAt = product.getCreatedAt();
        res.updatedAt = product.getUpdatedAt();
        return res;
    }
}
