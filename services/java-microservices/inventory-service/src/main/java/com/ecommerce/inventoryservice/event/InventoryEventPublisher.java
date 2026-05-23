package com.ecommerce.inventoryservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private static final String TOPIC_INVENTORY_RESERVED = "inventory.reserved";
    private static final String TOPIC_INVENTORY_DEPLETED = "inventory.depleted";

    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;

    public void publishReserved(UUID productId, String sku, int quantity,
                                int quantityAvailable, int quantityReserved, UUID referenceId) {
        InventoryEvent event = InventoryEvent.builder()
                .eventType("INVENTORY_RESERVED")
                .productId(productId)
                .sku(sku)
                .quantity(quantity)
                .quantityAvailable(quantityAvailable)
                .quantityReserved(quantityReserved)
                .referenceId(referenceId)
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(TOPIC_INVENTORY_RESERVED, productId.toString(), event);
        log.info("Published inventory.reserved event for product: {}, quantity: {}", productId, quantity);
    }

    public void publishDepleted(UUID productId, String sku, int quantity,
                                int quantityAvailable, int quantityReserved, UUID referenceId) {
        InventoryEvent event = InventoryEvent.builder()
                .eventType("INVENTORY_DEPLETED")
                .productId(productId)
                .sku(sku)
                .quantity(quantity)
                .quantityAvailable(quantityAvailable)
                .quantityReserved(quantityReserved)
                .referenceId(referenceId)
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(TOPIC_INVENTORY_DEPLETED, productId.toString(), event);
        log.info("Published inventory.depleted event for product: {}, quantity: {}", productId, quantity);
    }
}
