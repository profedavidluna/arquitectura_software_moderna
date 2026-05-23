package com.ecommerce.orderservice.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    @DisplayName("Should have all expected order statuses")
    void shouldHaveAllExpectedStatuses() {
        OrderStatus[] statuses = OrderStatus.values();
        assertThat(statuses).hasSize(7);
        assertThat(statuses).containsExactly(
                OrderStatus.PENDING,
                OrderStatus.CONFIRMED,
                OrderStatus.PROCESSING,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED,
                OrderStatus.CANCELLED,
                OrderStatus.REFUNDED
        );
    }

    @Test
    @DisplayName("Should convert status from string")
    void shouldConvertFromString() {
        assertThat(OrderStatus.valueOf("PENDING")).isEqualTo(OrderStatus.PENDING);
        assertThat(OrderStatus.valueOf("CONFIRMED")).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(OrderStatus.valueOf("CANCELLED")).isEqualTo(OrderStatus.CANCELLED);
    }
}
