package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.AddressRequest;
import com.ecommerce.userservice.dto.AddressResponse;
import com.ecommerce.userservice.exception.ErrorResponse;
import com.ecommerce.userservice.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/addresses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Addresses", description = "User address management endpoints")
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @Operation(summary = "Create address", description = "Add a new address for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Address created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AddressResponse> createAddress(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @Valid @RequestBody AddressRequest request) {
        log.info("POST /api/v1/users/{}/addresses", userId);
        AddressResponse response = addressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get user addresses", description = "Retrieve all addresses for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<AddressResponse>> getAddresses(
            @Parameter(description = "User UUID") @PathVariable UUID userId) {
        log.info("GET /api/v1/users/{}/addresses", userId);
        return ResponseEntity.ok(addressService.getAddressesByUserId(userId));
    }

    @PutMapping("/{addressId}")
    @Operation(summary = "Update address", description = "Update an existing address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Address not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AddressResponse> updateAddress(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @Parameter(description = "Address UUID") @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest request) {
        log.info("PUT /api/v1/users/{}/addresses/{}", userId, addressId);
        return ResponseEntity.ok(addressService.updateAddress(userId, addressId, request));
    }

    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete address", description = "Remove an address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Address deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteAddress(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @Parameter(description = "Address UUID") @PathVariable UUID addressId) {
        log.info("DELETE /api/v1/users/{}/addresses/{}", userId, addressId);
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{addressId}/default")
    @Operation(summary = "Set default address", description = "Set an address as the default shipping address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Default address set successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @Parameter(description = "Address UUID") @PathVariable UUID addressId) {
        log.info("PUT /api/v1/users/{}/addresses/{}/default", userId, addressId);
        return ResponseEntity.ok(addressService.setDefaultAddress(userId, addressId));
    }
}
