package com.ecommerce.orderservice.client;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private UUID paymentId;
    private String transactionId;
    private String status;
    private String message;
}
