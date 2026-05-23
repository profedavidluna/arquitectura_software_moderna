package com.ecommerce.userservice.event;

import com.ecommerce.userservice.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(KafkaTemplate.class)
public class UserEventPublisher {

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public void publishUserRegistered(UserEvent event) {
        log.info("Publishing user registered event for user: {}", event.getUserId());
        kafkaTemplate.send(KafkaConfig.USER_REGISTERED_TOPIC, event.getUserId().toString(), event);
    }

    public void publishUserUpdated(UserEvent event) {
        log.info("Publishing user updated event for user: {}", event.getUserId());
        kafkaTemplate.send(KafkaConfig.USER_UPDATED_TOPIC, event.getUserId().toString(), event);
    }
}
