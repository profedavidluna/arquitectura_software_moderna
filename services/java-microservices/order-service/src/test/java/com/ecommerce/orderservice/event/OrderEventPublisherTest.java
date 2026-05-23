package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    private OrderEventPublisher publisher;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        publisher = new OrderEventPublisher(kafkaTemplate);

        OrderItem item = OrderItem.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .productSku("SKU-001")
                .productName("Test Product")
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .subtotal(new BigDecimal("100.00"))
                .build();

        sampleOrder = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-20261105-001")
                .userId(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("113.99"))
                .currency("USD")
                .items(new ArrayList<>(List.of(item)))
                .build();

        CompletableFuture<SendResult<String, OrderEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(OrderEvent.class))).thenReturn(future);
    }

    @Test
    @DisplayName("Should publish order.created event")
    void shouldPublishOrderCreatedEvent() {
        publisher.publishOrderCreated(sampleOrder);

        ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(kafkaTemplate).send(eq("order.created"), eq(sampleOrder.getId().toString()), eventCaptor.capture());

        OrderEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(event.getOrderId()).isEqualTo(sampleOrder.getId());
        assertThat(event.getOrderNumber()).isEqualTo("ORD-20261105-001");
        assertThat(event.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("Should publish order.confirmed event")
    void shouldPublishOrderConfirmedEvent() {
        sampleOrder.setStatus(OrderStatus.CONFIRMED);
        publisher.publishOrderConfirmed(sampleOrder);

        ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(kafkaTemplate).send(eq("order.confirmed"), eq(sampleOrder.getId().toString()), eventCaptor.capture());

        OrderEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("ORDER_CONFIRMED");
    }

    @Test
    @DisplayName("Should publish order.shipped event")
    void shouldPublishOrderShippedEvent() {
        sampleOrder.setStatus(OrderStatus.SHIPPED);
        publisher.publishOrderShipped(sampleOrder);

        verify(kafkaTemplate).send(eq("order.shipped"), eq(sampleOrder.getId().toString()), any(OrderEvent.class));
    }

    @Test
    @DisplayName("Should publish order.cancelled event")
    void shouldPublishOrderCancelledEvent() {
        sampleOrder.setStatus(OrderStatus.CANCELLED);
        publisher.publishOrderCancelled(sampleOrder);

        ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(kafkaTemplate).send(eq("order.cancelled"), eq(sampleOrder.getId().toString()), eventCaptor.capture());

        OrderEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("ORDER_CANCELLED");
    }
}
