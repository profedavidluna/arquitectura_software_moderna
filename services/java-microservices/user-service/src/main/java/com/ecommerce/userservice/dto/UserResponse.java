package com.ecommerce.userservice.dto;

import com.ecommerce.userservice.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User response payload")
public class UserResponse {

    @Schema(description = "User unique identifier")
    private UUID id;

    @Schema(description = "User email address")
    private String email;

    @Schema(description = "Unique username")
    private String username;

    @Schema(description = "User first name")
    private String firstName;

    @Schema(description = "User last name")
    private String lastName;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "URL to user profile image")
    private String avatarUrl;

    @Schema(description = "User role")
    private Role role;

    @Schema(description = "Whether the account is active")
    private Boolean isActive;

    @Schema(description = "Whether the email has been verified")
    private Boolean emailVerified;

    @Schema(description = "Last login timestamp")
    private LocalDateTime lastLoginAt;

    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
