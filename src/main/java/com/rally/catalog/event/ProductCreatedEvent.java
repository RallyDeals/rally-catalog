package com.rally.catalog.event;

public class ProductCreatedEvent {

    private String productId;
    private Integer initialStock;

    public ProductCreatedEvent() {
    }

    public ProductCreatedEvent(String productId, Integer initialStock) {
        this.productId = productId;
        this.initialStock = initialStock;
    }

    public String getProductId() {
        return productId;
    }

    public Integer getInitialStock() {
        return initialStock;
    }
}
