package com.rally.catalog.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SellerSummary {
    private String sellerId;
    private int totalProducts;
    private int totalPendingProducts;
}
