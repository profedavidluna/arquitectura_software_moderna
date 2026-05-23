package com.ecommerce.product.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Configuration - Product Service
 * 
 * <p><b>SOA - Enterprise Service Bus Configuration</b>:
 * This configuration sets up the Kafka topics used by the Product Service.
 * In SOA, the ESB is the central communication backbone.</p>
 * 
 * <p>Topics are created with a single partition and replication factor of 1
 * for development. In production, these would be configured for high availability.</p>
 */
@Configuration
public class KafkaConfig {

    /**
     * Creates the "product.created" topic.
     * This topic is consumed by the Inventory Service to automatically
     * create inventory entries for new products.
     */
    @Bean
    public NewTopic productCreatedTopic() {
        return TopicBuilder.name("product.created")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
