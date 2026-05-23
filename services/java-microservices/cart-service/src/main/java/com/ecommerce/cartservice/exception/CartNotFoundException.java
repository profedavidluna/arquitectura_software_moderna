package com.ecommerce.cartservice.exception;

import java.util.UUID;

public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException(UUID cartId) {
        super("Cart not found with id: " + cartId);
    }

    public CartNotFoundException(String message) {
        super(message);
    }
}
