package com.rally.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductUpdateRequest {

    private String name;

    private String description;

    private String categoryId;

    @DecimalMin(value = "0.01", message = "Base price must be positive")
    private BigDecimal basePrice;

    private String imageUrl;

    @Size(max = 10, message = "A product can have at most 10 images")
    private List<String> images;
}
