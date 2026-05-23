package com.ecommerce.paymentservice.config;

import com.ecommerce.paymentservice.event.PaymentEvent;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Test configuration that provides a mock KafkaTemplate
 * when Kafka auto-configuration is excluded.
 */
@TestConfiguration
@Profile("test")
public class TestKafkaConfig {

    @SuppressWarnings("unchecked")
    @Bean
    public KafkaTemplate<String, PaymentEvent> kafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }
}
