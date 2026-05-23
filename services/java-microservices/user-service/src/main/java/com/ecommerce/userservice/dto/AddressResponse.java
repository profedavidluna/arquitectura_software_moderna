package com.ecommerce.userservice.dto;

import com.ecommerce.userservice.entity.AddressLabel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Address response payload")
public class AddressResponse {

    @Schema(description = "Address unique identifier")
    private UUID id;

    @Schema(description = "User ID who owns this address")
    private UUID userId;

    @Schema(description = "Address label")
    private AddressLabel label;

    @Schema(description = "Primary street address")
    private String street;

    @Schema(description = "Secondary address line")
    private String streetLine2;

    @Schema(description = "City name")
    private String city;

    @Schema(description = "State or province")
    private String state;

    @Schema(description = "Postal/ZIP code")
    private String postalCode;

    @Schema(description = "Country name or ISO code")
    private String country;

    @Schema(description = "Whether this is the default address")
    private Boolean isDefault;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
