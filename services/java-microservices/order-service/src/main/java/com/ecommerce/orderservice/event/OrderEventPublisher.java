package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private static final String ORDER_CREATED_TOPIC = "order.created";
    private static final String ORDER_CONFIRMED_TOPIC = "order.confirmed";
    private static final String ORDER_SHIPPED_TOPIC = "order.shipped";
    private static final String ORDER_CANCELLED_TOPIC = "order.cancelled";

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publishOrderCreated(Order order) {
        OrderEvent event = buildOrderEvent(order, "ORDER_CREATED");
        publish(ORDER_CREATED_TOPIC, order.getId().toString(), event);
    }

    public void publishOrderConfirmed(Order order) {
        OrderEvent event = buildOrderEvent(order, "ORDER_CONFIRMED");
        publish(ORDER_CONFIRMED_TOPIC, order.getId().toString(), event);
    }

    public void publishOrderShipped(Order order) {
        OrderEvent event = buildOrderEvent(order, "ORDER_SHIPPED");
        publish(ORDER_SHIPPED_TOPIC, order.getId().toString(), event);
    }

    public void publishOrderCancelled(Order order) {
        OrderEvent event = buildOrderEvent(order, "ORDER_CANCELLED");
        publish(ORDER_CANCELLED_TOPIC, order.getId().toString(), event);
    }

    private void publish(String topic, String key, OrderEvent event) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event to topic {}: {}", topic, ex.getMessage());
                    } else {
                        log.info("Published event to topic {}: orderId={}", topic, event.getOrderId());
                    }
                });
    }

    private OrderEvent buildOrderEvent(Order order, String eventType) {
        return OrderEvent.builder()
                .eventType(eventType)
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .items(order.getItems().stream()
                        .map(this::toItemEvent)
                        .collect(Collectors.toList()))
                .timestamp(LocalDateTime.now())
                .build();
    }

    private OrderEvent.OrderItemEvent toItemEvent(OrderItem item) {
        return OrderEvent.OrderItemEvent.builder()
                .productId(item.getProductId())
                .productSku(item.getProductSku())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .build();
    }
}
