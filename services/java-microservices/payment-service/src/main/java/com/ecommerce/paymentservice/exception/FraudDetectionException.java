package com.ecommerce.paymentservice.exception;

public class FraudDetectionException extends RuntimeException {

    public FraudDetectionException(String message) {
        super(message);
    }
}
