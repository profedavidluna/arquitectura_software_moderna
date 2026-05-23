package com.ecommerce.orderservice.exception;

import com.ecommerce.orderservice.entity.OrderStatus;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(OrderStatus currentStatus, OrderStatus targetStatus) {
        super(String.format("Cannot transition order from %s to %s", currentStatus, targetStatus));
    }

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
