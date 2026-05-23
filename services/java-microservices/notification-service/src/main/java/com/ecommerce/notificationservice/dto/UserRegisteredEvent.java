package com.ecommerce.notificationservice.dto;

import jakarta.validation.constraints.Email;
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
public class UserRegisteredEvent {

    @NotBlank
    private String userId;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String username;

    private Instant registeredAt;
}
