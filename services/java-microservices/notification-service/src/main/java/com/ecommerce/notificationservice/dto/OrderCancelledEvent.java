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
public class OrderCancelledEvent {

    @NotBlank
    private String orderId;

    @NotBlank
    private String userId;

    private Instant cancelledAt;

    private String reason;

    private String email;
}
