package com.rally.catalog.messaging;

import com.rally.catalog.messaging.contract.CatalogMessageHeaders;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class KafkaProducerCorrelationInterceptor implements ProducerInterceptor<String, Object> {

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        String correlationId = MDC.get(CatalogMessageHeaders.CORRELATION_ID);
        if (correlationId != null && !correlationId.isBlank()
                && record.headers().lastHeader(CatalogMessageHeaders.CORRELATION_ID) == null) {
            record.headers().add(CatalogMessageHeaders.CORRELATION_ID,
                    correlationId.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        if (metadata != null) {
            log.debug("Kafka record ack: topic {} partition {} offset {}",
                    metadata.topic(), metadata.partition(), metadata.offset());
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}