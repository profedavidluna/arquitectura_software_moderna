package com.ecommerce.order.infrastructure.messaging;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.infrastructure.messaging.events.OrderCreatedEvent;
import com.ecommerce.order.infrastructure.messaging.events.OrderCreatedEvent.OrderItemPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order Event Publisher - Infrastructure Layer
 * 
 * <p><b>Observer Pattern</b>: Publishes order lifecycle events to Kafka topics.
 * Multiple services can subscribe to these events independently.</p>
 * 
 * <p><b>Saga Pattern</b>: Each published event represents a step in the
 * distributed transaction saga.</p>
 * 
 * <p>Topics published:</p>
 * <ul>
 *   <li>order.created - When a new order is placed (initiates saga)</li>
 *   <li>order.confirmed - When stock is reserved and order is confirmed</li>
 *   <li>order.cancelled - When order is cancelled (triggers compensation)</li>
 * </ul>
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private static final String TOPIC_ORDER_CREATED = "order.created";
    private static final String TOPIC_ORDER_CONFIRMED = "order.confirmed";
    private static final String TOPIC_ORDER_CANCELLED = "order.cancelled";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes OrderCreatedEvent - initiates the order saga.
     */
    public void publishOrderCreated(Order order) {
        List<OrderItemPayload> itemPayloads = order.getItems().stream()
                .map(item -> new OrderItemPayload(
                        item.getProductId().toString(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .collect(Collectors.toList());

        OrderCreatedEvent event = OrderCreatedEvent.create(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                itemPayloads
        );

        String key = order.getId().toString();
        log.info("Publishing OrderCreatedEvent: orderId={}, items={}",
                order.getId(), order.getItems().size());

        kafkaTemplate.send(TOPIC_ORDER_CREATED, key, event);
    }

    /**
     * Publishes order confirmed event.
     */
    public void publishOrderConfirmed(Order order) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "ORDER_CONFIRMED");
        event.put("timestamp", java.time.LocalDateTime.now().toString());
        event.put("orderId", order.getId().toString());
        event.put("userId", order.getUserId().toString());

        String key = order.getId().toString();
        log.info("Publishing OrderConfirmedEvent: orderId={}", order.getId());

        kafkaTemplate.send(TOPIC_ORDER_CONFIRMED, key, event);
    }

    /**
     * Publishes order cancelled event - triggers compensation in other services.
     */
    public void publishOrderCancelled(Order order) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "ORDER_CANCELLED");
        event.put("timestamp", java.time.LocalDateTime.now().toString());
        event.put("orderId", order.getId().toString());
        event.put("userId", order.getUserId().toString());

        // Include items so Inventory Service knows what to release
        List<Map<String, Object>> items = order.getItems().stream()
                .map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("productId", item.getProductId().toString());
                    itemMap.put("quantity", item.getQuantity());
                    return itemMap;
                })
                .collect(Collectors.toList());
        event.put("items", items);

        String key = order.getId().toString();
        log.info("Publishing OrderCancelledEvent: orderId={}", order.getId());

        kafkaTemplate.send(TOPIC_ORDER_CANCELLED, key, event);
    }
}
