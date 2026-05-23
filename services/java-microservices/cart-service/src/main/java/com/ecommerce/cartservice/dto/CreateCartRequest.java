package com.ecommerce.cartservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCartRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    private String sessionId;

    private String currency;
}
