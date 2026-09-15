package com.rally.catalog.config.rest;

import com.rally.catalog.messaging.contract.CatalogMessageHeaders;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String correlationId = MDC.get(CatalogMessageHeaders.CORRELATION_ID);
        request.getHeaders().set(CatalogMessageHeaders.CORRELATION_ID,
                correlationId == null || correlationId.isBlank()
                        ? UUID.randomUUID().toString()
                        : correlationId);

        long startNanos = System.nanoTime();
        try {
            ClientHttpResponse response = execution.execute(request, body);
            log.info("Outbound {} {} -> {} ({} ms)", request.getMethod(), request.getURI(),
                    response.getStatusCode().value(), elapsedMillis(startNanos));
            return response;
        } catch (IOException e) {
            log.warn("Outbound {} {} failed after {} ms: {}", request.getMethod(), request.getURI(),
                    elapsedMillis(startNanos), e.getMessage());
            throw e;
        }
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}