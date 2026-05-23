package com.ecommerce.cartservice.exception;

public class InvalidCouponException extends RuntimeException {

    public InvalidCouponException(String couponCode) {
        super("Invalid or expired coupon code: " + couponCode);
    }
}
