package com.ecommerce.cartservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String CART_EVENTS_TOPIC = "cart-events";
    public static final String CART_ABANDONED_TOPIC = "cart-abandoned";

    @Bean
    public NewTopic cartEventsTopic() {
        return TopicBuilder.name(CART_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic cartAbandonedTopic() {
        return TopicBuilder.name(CART_ABANDONED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
