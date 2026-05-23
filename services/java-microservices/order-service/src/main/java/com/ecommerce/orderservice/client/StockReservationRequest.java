package com.ecommerce.orderservice.client;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservationRequest {

    private UUID orderId;
    private List<ReservationRequest> items;
}
