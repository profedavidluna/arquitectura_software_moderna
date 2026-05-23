package com.ecommerce.productservice.event;

import com.ecommerce.productservice.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@ConditionalOnBean(KafkaTemplate.class)
@RequiredArgsConstructor
@Slf4j
public class ProductEventPublisher {

    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;

    public void publishProductCreated(UUID productId, String name, String sku, BigDecimal price, UUID categoryId) {
        ProductEvent event = ProductEvent.builder()
                .productId(productId)
                .eventType(ProductEvent.EventType.CREATED.name())
                .name(name)
                .sku(sku)
                .price(price)
                .categoryId(categoryId)
                .isActive(true)
                .timestamp(LocalDateTime.now())
                .build();

        sendEvent(KafkaConfig.PRODUCT_CREATED_TOPIC, productId.toString(), event);
    }

    public void publishProductUpdated(UUID productId, String name, String sku, BigDecimal price, UUID categoryId) {
        ProductEvent event = ProductEvent.builder()
                .productId(productId)
                .eventType(ProductEvent.EventType.UPDATED.name())
                .name(name)
                .sku(sku)
                .price(price)
                .categoryId(categoryId)
                .isActive(true)
                .timestamp(LocalDateTime.now())
                .build();

        sendEvent(KafkaConfig.PRODUCT_UPDATED_TOPIC, productId.toString(), event);
    }

    public void publishProductDeleted(UUID productId) {
        ProductEvent event = ProductEvent.builder()
                .productId(productId)
                .eventType(ProductEvent.EventType.DELETED.name())
                .isActive(false)
                .timestamp(LocalDateTime.now())
                .build();

        sendEvent(KafkaConfig.PRODUCT_DELETED_TOPIC, productId.toString(), event);
    }

    private void sendEvent(String topic, String key, ProductEvent event) {
        try {
            kafkaTemplate.send(topic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event to topic {}: {}", topic, ex.getMessage());
                        } else {
                            log.info("Published event to topic {}: productId={}", topic, event.getProductId());
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing event to topic {}: {}", topic, e.getMessage());
        }
    }
}
