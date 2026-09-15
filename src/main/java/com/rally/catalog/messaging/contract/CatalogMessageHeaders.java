package com.rally.catalog.messaging.contract;

public final class CatalogMessageHeaders {

    public static final String ID = "X-Id";
    public static final String TYPE = "X-Type";
    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String TRACEPARENT = "traceparent";

    private CatalogMessageHeaders() {
    }
}