package com.rally.catalog.event;

import java.time.Instant;
import java.util.UUID;

public class SellerBannedEvent {

    private UUID sellerId;
    private String reason;
    private Instant bannedAt;

    public SellerBannedEvent() {
    }

    public SellerBannedEvent(UUID sellerId, String reason, Instant bannedAt) {
        this.sellerId = sellerId;
        this.reason = reason;
        this.bannedAt = bannedAt;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getBannedAt() {
        return bannedAt;
    }
}
