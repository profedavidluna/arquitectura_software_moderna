package com.ecommerce.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelOrderRequest {

    @NotBlank(message = "Cancellation reason is required")
    private String reason;

    private UUID cancelledBy;
}
