package com.ecommerce.userservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String USER_REGISTERED_TOPIC = "user.registered";
    public static final String USER_UPDATED_TOPIC = "user.updated";

    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name(USER_REGISTERED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userUpdatedTopic() {
        return TopicBuilder.name(USER_UPDATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
