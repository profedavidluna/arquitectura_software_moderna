package com.ecommerce.layered.business.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(int available, int requested) {
        super("Insufficient stock. Available: " + available + ", Requested: " + requested);
    }
}
