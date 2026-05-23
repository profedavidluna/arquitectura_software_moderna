package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.entity.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Shipping address is required")
    @Valid
    private Address shippingAddress;

    private Address billingAddress;

    private String notes;

    private String paymentMethod;

    private String currency;
}
