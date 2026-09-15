package com.rally.catalog.config;

import com.rally.catalog.event.ProductCreatedEvent;
import com.rally.catalog.event.ProductDeletedEvent;
import com.rally.catalog.messaging.KafkaProducerCorrelationInterceptor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> producerProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        properties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        properties.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                KafkaProducerCorrelationInterceptor.class.getName());
        return properties;
    }

    @Bean
    public ProducerFactory<String, ProductCreatedEvent> productCreatedProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProperties());
    }

    @Bean
    public KafkaTemplate<String, ProductCreatedEvent> productCreatedKafkaTemplate() {
        KafkaTemplate<String, ProductCreatedEvent> template =
                new KafkaTemplate<>(productCreatedProducerFactory());
        template.setObservationEnabled(true);
        return template;
    }

    @Bean
    public ProducerFactory<String, ProductDeletedEvent> productDeletedProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProperties());
    }

    @Bean
    public KafkaTemplate<String, ProductDeletedEvent> productDeletedKafkaTemplate() {
        KafkaTemplate<String, ProductDeletedEvent> template =
                new KafkaTemplate<>(productDeletedProducerFactory());
        template.setObservationEnabled(true);
        return template;
    }
}
