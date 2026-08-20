package com.rally.catalog.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rally.catalog.entity.ProductActiveDeal;
import com.rally.catalog.repository.ProductActiveDealRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
public class DealEventListener {

    private static final List<String> ACTIVE_STATUSES = List.of("PENDING", "ACTIVE");

    private final ObjectMapper objectMapper;
    private final ProductActiveDealRepository repository;

    public DealEventListener(ObjectMapper objectMapper, ProductActiveDealRepository repository) {
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    @KafkaListener(
            topics = "deal-events",
            containerFactory = "dealEventKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleDealEvent(String raw) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            String eventType = node.path("event_type").asText();
            String dealId = node.path("deal_id").asText();
            String productId = node.path("product_id").asText();

            if (eventType == null || eventType.isBlank()) {
                log.warn("Received deal event with no event_type, skipping");
                return;
            }

            switch (eventType) {
                case "deal.created" -> handleCreated(node, dealId, productId);
                case "deal.activated" -> handleActivated(node, dealId);
                case "deal.cancelled" -> handleCancelled(dealId, productId);
                case "deal.succeeded", "deal.failed" -> handleResolved(node, dealId, eventType);
                default -> log.debug("Ignoring unknown deal event type={}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process deal event: {}", e.getMessage(), e);
        }
    }

    private void handleCreated(JsonNode node, String dealId, String productId) {
        ProductActiveDeal pad = new ProductActiveDeal();
        pad.setDealId(dealId);
        pad.setProductId(productId);
        pad.setDealPrice(node.path("deal_price").decimalValue());
        pad.setDealStock(node.path("deal_stock").asInt());
        pad.setCurrentParticipants(0);
        pad.setMinParticipants(node.path("min_participants").asInt());
        pad.setDurationMinutes(node.path("duration_minutes").asInt(0));
        pad.setStatus("PENDING");
        pad.setEndTime(null);
        pad.setCreatedAt(OffsetDateTime.now());

        repository.save(pad);
        log.info("Stored active deal created: dealId={} productId={}", dealId, productId);
    }

    private void handleCancelled(String dealId, String productId) {
        repository.deleteById(dealId);
        log.info("Removed cancelled deal: dealId={} productId={}", dealId, productId);
    }

    private void handleActivated(JsonNode node, String dealId) {
        repository.findById(dealId).ifPresentOrElse(pad -> {
            pad.setStatus("ACTIVE");
            pad.setCurrentParticipants(node.path("current_participants").asInt(pad.getCurrentParticipants()));
            if (node.has("end_time") && !node.get("end_time").isNull()) {
                pad.setEndTime(OffsetDateTime.parse(node.get("end_time").asText()));
            }
            repository.save(pad);
            log.info("Activated deal: dealId={} PENDING→ACTIVE", dealId);
        }, () -> log.warn("Activated deal {} not found in local table", dealId));
    }

    private void handleResolved(JsonNode node, String dealId, String eventType) {
        String status = eventType.equals("deal.succeeded") ? "SUCCEEDED" : "FAILED";
        repository.findById(dealId).ifPresentOrElse(pad -> {
            pad.setStatus(status);
            pad.setCurrentParticipants(node.path("current_participants").asInt(pad.getCurrentParticipants()));
            pad.setDealStock(node.path("deal_stock").asInt(pad.getDealStock()));
            if (node.has("end_time") && !node.get("end_time").isNull()) {
                pad.setEndTime(OffsetDateTime.parse(node.get("end_time").asText()));
            }
            repository.save(pad);
            log.info("Updated deal status: dealId={} status={}", dealId, status);
        }, () -> {
            log.warn("Resolved deal {} not found in local table — storing anyway", dealId);
            ProductActiveDeal pad = new ProductActiveDeal();
            pad.setDealId(dealId);
            pad.setProductId(node.path("product_id").asText());
            pad.setDealPrice(node.path("deal_price").decimalValue());
            pad.setDealStock(node.path("deal_stock").asInt());
            pad.setCurrentParticipants(node.path("current_participants").asInt());
            pad.setMinParticipants(node.path("min_participants").asInt());
            pad.setDurationMinutes(0);
            pad.setStatus(status);
            if (node.has("end_time") && !node.get("end_time").isNull()) {
                pad.setEndTime(OffsetDateTime.parse(node.get("end_time").asText()));
            }
            pad.setCreatedAt(OffsetDateTime.now());
            repository.save(pad);
        });
    }
}
