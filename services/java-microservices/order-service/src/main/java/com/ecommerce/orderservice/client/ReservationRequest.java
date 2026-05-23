package com.ecommerce.orderservice.client;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequest {

    private UUID productId;
    private String productSku;
    private Integer quantity;
}
