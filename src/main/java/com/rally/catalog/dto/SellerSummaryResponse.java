package com.rally.catalog.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SellerSummaryResponse {
    private List<SellerSummary> sellers;
}
