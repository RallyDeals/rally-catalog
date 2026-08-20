package com.rally.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "product_active_deals")
public class ProductActiveDeal {

    @Id
    @Column(name = "deal_id", length = 36)
    private String dealId;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "deal_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dealPrice;

    @Column(name = "deal_stock", nullable = false)
    private int dealStock;

    @Column(name = "current_participants", nullable = false)
    private int currentParticipants;

    @Column(name = "min_participants", nullable = false)
    private int minParticipants;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "duration_minutes")
    private int durationMinutes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
