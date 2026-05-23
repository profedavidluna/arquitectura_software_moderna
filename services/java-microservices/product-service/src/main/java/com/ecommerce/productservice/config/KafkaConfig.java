package com.ecommerce.productservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String PRODUCT_CREATED_TOPIC = "product.created";
    public static final String PRODUCT_UPDATED_TOPIC = "product.updated";
    public static final String PRODUCT_DELETED_TOPIC = "product.deleted";

    @Bean
    public NewTopic productCreatedTopic() {
        return TopicBuilder.name(PRODUCT_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productUpdatedTopic() {
        return TopicBuilder.name(PRODUCT_UPDATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productDeletedTopic() {
        return TopicBuilder.name(PRODUCT_DELETED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
