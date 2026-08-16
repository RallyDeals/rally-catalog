package com.rally.catalog.event;

public class ProductDeletedEvent {

    private String productId;

    public ProductDeletedEvent() {
    }

    public ProductDeletedEvent(String productId) {
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}
