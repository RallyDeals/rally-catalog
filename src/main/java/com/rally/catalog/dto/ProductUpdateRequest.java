package com.rally.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductUpdateRequest {

    private String name;

    private String description;

    private String categoryId;

    @DecimalMin(value = "0.01", message = "Base price must be positive")
    private BigDecimal basePrice;

    private String imageUrl;
}
