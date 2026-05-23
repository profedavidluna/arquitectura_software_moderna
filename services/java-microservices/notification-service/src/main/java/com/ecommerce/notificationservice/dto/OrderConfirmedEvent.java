package com.ecommerce.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedEvent {

    @NotBlank
    private String orderId;

    @NotBlank
    private String userId;

    private String status;

    private Instant confirmedAt;

    private String email;
}
