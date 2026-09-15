package com.rally.catalog.filter;

import com.rally.catalog.messaging.contract.CatalogMessageHeaders;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.BaggageManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private final BaggageManager baggageManager;

    public CorrelationIdFilter(BaggageManager baggageManager) {
        this.baggageManager = baggageManager;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(CatalogMessageHeaders.CORRELATION_ID);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(CatalogMessageHeaders.CORRELATION_ID, correlationId);

        try (BaggageInScope ignored = baggageManager.createBaggageInScope(
                CatalogMessageHeaders.CORRELATION_ID, correlationId)) {

            response.addHeader(CatalogMessageHeaders.CORRELATION_ID, correlationId);

            try {
                filterChain.doFilter(request, response);
            } finally {
                MDC.remove(CatalogMessageHeaders.CORRELATION_ID);
            }
        }
    }
}