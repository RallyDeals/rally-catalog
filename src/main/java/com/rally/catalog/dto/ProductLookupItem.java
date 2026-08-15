package com.rally.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductLookupItem {

    private String id;
    private String name;
    private BigDecimal basePrice;
    private String imageUrl;
    private String sellerId;
}
