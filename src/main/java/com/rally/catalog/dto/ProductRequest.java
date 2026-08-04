package com.rally.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Category is required")
    private String categoryId;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Base price must be positive")
    private BigDecimal basePrice;

    private String imageUrl;
}
