package com.ecommerce.userservice.dto;

import com.ecommerce.userservice.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for updating a user")
public class UpdateUserRequest {

    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "User email address", example = "john.doe@example.com")
    private String email;

    @Pattern(regexp = "^[a-zA-Z0-9_]{3,100}$", message = "Username must be 3-100 alphanumeric characters or underscores")
    @Schema(description = "Unique username", example = "john_doe")
    private String username;

    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Schema(description = "User first name", example = "John")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Schema(description = "User last name", example = "Doe")
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9\\-() ]{7,20}$", message = "Phone must be a valid phone number")
    @Schema(description = "Phone number in international format", example = "+1-555-123-4567")
    private String phone;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    @Schema(description = "URL to user profile image")
    private String avatarUrl;

    @Schema(description = "User role", example = "CUSTOMER")
    private Role role;
}
