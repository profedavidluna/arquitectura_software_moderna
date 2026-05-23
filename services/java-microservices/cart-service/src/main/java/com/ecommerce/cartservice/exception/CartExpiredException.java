package com.ecommerce.cartservice.exception;

import java.util.UUID;

public class CartExpiredException extends RuntimeException {

    public CartExpiredException(UUID cartId) {
        super("Cart has expired: " + cartId);
    }
}
