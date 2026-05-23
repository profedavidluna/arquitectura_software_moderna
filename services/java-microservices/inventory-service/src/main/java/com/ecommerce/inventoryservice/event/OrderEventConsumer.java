package com.ecommerce.inventoryservice.event;

import com.ecommerce.inventoryservice.dto.ReleaseStockRequest;
import com.ecommerce.inventoryservice.dto.ReserveStockRequest;
import com.ecommerce.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(topics = "order.created", groupId = "inventory-service-group")
    public void handleOrderCreated(OrderEvent event) {
        log.info("Received order.created event for order: {}", event.getOrderId());

        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("Order event has no items, skipping: {}", event.getOrderId());
            return;
        }

        for (OrderEvent.OrderItem item : event.getItems()) {
            try {
                ReserveStockRequest request = ReserveStockRequest.builder()
                        .quantity(item.getQuantity())
                        .referenceId(event.getOrderId())
                        .reason("Auto-reserve for order " + event.getOrderId())
                        .build();

                inventoryService.reserveStock(item.getProductId(), request);
                log.info("Reserved {} units for product {} (order: {})",
                        item.getQuantity(), item.getProductId(), event.getOrderId());
            } catch (Exception e) {
                log.error("Failed to reserve stock for product {} (order: {}): {}",
                        item.getProductId(), event.getOrderId(), e.getMessage());
            }
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "inventory-service-group")
    public void handleOrderCancelled(OrderEvent event) {
        log.info("Received order.cancelled event for order: {}", event.getOrderId());

        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("Order event has no items, skipping: {}", event.getOrderId());
            return;
        }

        for (OrderEvent.OrderItem item : event.getItems()) {
            try {
                ReleaseStockRequest request = ReleaseStockRequest.builder()
                        .quantity(item.getQuantity())
                        .referenceId(event.getOrderId())
                        .reason("Auto-release for cancelled order " + event.getOrderId())
                        .build();

                inventoryService.releaseStock(item.getProductId(), request);
                log.info("Released {} units for product {} (cancelled order: {})",
                        item.getQuantity(), item.getProductId(), event.getOrderId());
            } catch (Exception e) {
                log.error("Failed to release stock for product {} (order: {}): {}",
                        item.getProductId(), event.getOrderId(), e.getMessage());
            }
        }
    }
}
