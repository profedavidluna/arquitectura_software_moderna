package com.ecommerce.userservice.dto;

import com.ecommerce.userservice.entity.AddressLabel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for creating or updating an address")
public class AddressRequest {

    @Schema(description = "Address label", example = "Home")
    private AddressLabel label;

    @NotBlank(message = "Street is required")
    @Size(max = 255, message = "Street must not exceed 255 characters")
    @Schema(description = "Primary street address", example = "123 Main St")
    private String street;

    @Size(max = 255, message = "Street line 2 must not exceed 255 characters")
    @Schema(description = "Secondary address line", example = "Apt 4B")
    private String streetLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Schema(description = "City name", example = "New York")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    @Schema(description = "State or province", example = "NY")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    @Schema(description = "Postal/ZIP code", example = "10001")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country must not exceed 100 characters")
    @Schema(description = "Country name or ISO code", example = "US")
    private String country;

    @Schema(description = "Whether this is the default address", example = "false")
    private Boolean isDefault;
}
