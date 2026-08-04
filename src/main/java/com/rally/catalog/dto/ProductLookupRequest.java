package com.rally.catalog.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductLookupRequest {

    @NotEmpty(message = "productIds must not be empty")
    @Size(max = 50, message = "At most 50 product IDs per lookup")
    private List<String> productIds;
}
