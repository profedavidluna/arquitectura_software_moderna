package com.ecommerce.orderservice.client;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {

    private boolean reserved;
    private String message;
}
