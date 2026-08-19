package com.rally.catalog.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DealActiveSummary {

    private String dealId;
    private BigDecimal dealPrice;
    private int dealStock;
    private int currentParticipants;
    private int minParticipants;
    private String status;
    private OffsetDateTime endTime;
    private int durationMinutes;
}
