package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.AddressRequest;
import com.ecommerce.userservice.dto.AddressResponse;
import com.ecommerce.userservice.entity.AddressLabel;
import com.ecommerce.userservice.exception.GlobalExceptionHandler;
import com.ecommerce.userservice.exception.ResourceNotFoundException;
import com.ecommerce.userservice.service.AddressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    @Mock
    private AddressService addressService;

    @InjectMocks
    private AddressController addressController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userId;
    private UUID addressId;
    private AddressResponse testAddressResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(addressController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        testAddressResponse = AddressResponse.builder()
                .id(addressId)
                .userId(userId)
                .label(AddressLabel.Home)
                .street("123 Main St")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("US")
                .isDefault(false)
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/users/{userId}/addresses")
    class CreateAddressEndpoint {

        @Test
        @DisplayName("Should create address and return 201")
        void shouldCreateAddress() throws Exception {
            AddressRequest request = AddressRequest.builder()
                    .street("123 Main St")
                    .city("New York")
                    .state("NY")
                    .postalCode("10001")
                    .country("US")
                    .build();

            when(addressService.createAddress(eq(userId), any(AddressRequest.class)))
                    .thenReturn(testAddressResponse);

            mockMvc.perform(post("/api/v1/users/{userId}/addresses", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.street").value("123 Main St"));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            AddressRequest request = AddressRequest.builder()
                    .street("123 Main St")
                    .city("New York")
                    .postalCode("10001")
                    .country("US")
                    .build();

            when(addressService.createAddress(eq(userId), any(AddressRequest.class)))
                    .thenThrow(new ResourceNotFoundException("User", "id", userId));

            mockMvc.perform(post("/api/v1/users/{userId}/addresses", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}/addresses")
    class GetAddressesEndpoint {

        @Test
        @DisplayName("Should return addresses for user")
        void shouldReturnAddresses() throws Exception {
            when(addressService.getAddressesByUserId(userId)).thenReturn(List.of(testAddressResponse));

            mockMvc.perform(get("/api/v1/users/{userId}/addresses", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].street").value("123 Main St"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{userId}/addresses/{addressId}")
    class UpdateAddressEndpoint {

        @Test
        @DisplayName("Should update address and return 200")
        void shouldUpdateAddress() throws Exception {
            AddressRequest request = AddressRequest.builder()
                    .street("456 Oak Ave")
                    .city("Los Angeles")
                    .state("CA")
                    .postalCode("90001")
                    .country("US")
                    .build();

            when(addressService.updateAddress(eq(userId), eq(addressId), any(AddressRequest.class)))
                    .thenReturn(testAddressResponse);

            mockMvc.perform(put("/api/v1/users/{userId}/addresses/{addressId}", userId, addressId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/{userId}/addresses/{addressId}")
    class DeleteAddressEndpoint {

        @Test
        @DisplayName("Should delete address and return 204")
        void shouldDeleteAddress() throws Exception {
            doNothing().when(addressService).deleteAddress(userId, addressId);

            mockMvc.perform(delete("/api/v1/users/{userId}/addresses/{addressId}", userId, addressId))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{userId}/addresses/{addressId}/default")
    class SetDefaultAddressEndpoint {

        @Test
        @DisplayName("Should set default address and return 200")
        void shouldSetDefaultAddress() throws Exception {
            when(addressService.setDefaultAddress(userId, addressId)).thenReturn(testAddressResponse);

            mockMvc.perform(put("/api/v1/users/{userId}/addresses/{addressId}/default", userId, addressId))
                    .andExpect(status().isOk());
        }
    }
}
