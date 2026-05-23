package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.AddressRequest;
import com.ecommerce.userservice.dto.AddressResponse;
import com.ecommerce.userservice.entity.Address;
import com.ecommerce.userservice.entity.AddressLabel;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.ResourceNotFoundException;
import com.ecommerce.userservice.mapper.UserMapper;
import com.ecommerce.userservice.repository.AddressRepository;
import com.ecommerce.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AddressServiceImpl addressService;

    private UUID userId;
    private UUID addressId;
    private User testUser;
    private Address testAddress;
    private AddressResponse testAddressResponse;
    private AddressRequest addressRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("john@example.com")
                .username("john_doe")
                .build();

        testAddress = Address.builder()
                .id(addressId)
                .user(testUser)
                .label(AddressLabel.Home)
                .street("123 Main St")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("US")
                .isDefault(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

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

        addressRequest = AddressRequest.builder()
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
    @DisplayName("Create Address Tests")
    class CreateAddressTests {

        @Test
        @DisplayName("Should create address successfully")
        void shouldCreateAddressSuccessfully() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userMapper.toAddressEntity(any(AddressRequest.class))).thenReturn(testAddress);
            when(addressRepository.save(any(Address.class))).thenReturn(testAddress);
            when(userMapper.toAddressResponse(any(Address.class))).thenReturn(testAddressResponse);

            AddressResponse result = addressService.createAddress(userId, addressRequest);

            assertThat(result).isNotNull();
            assertThat(result.getStreet()).isEqualTo("123 Main St");
            verify(addressRepository).save(any(Address.class));
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> addressService.createAddress(userId, addressRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Addresses Tests")
    class GetAddressesTests {

        @Test
        @DisplayName("Should return addresses for user")
        void shouldReturnAddressesForUser() {
            when(userRepository.existsById(userId)).thenReturn(true);
            when(addressRepository.findByUserId(userId)).thenReturn(List.of(testAddress));
            when(userMapper.toAddressResponse(testAddress)).thenReturn(testAddressResponse);

            List<AddressResponse> result = addressService.getAddressesByUserId(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStreet()).isEqualTo("123 Main St");
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.existsById(userId)).thenReturn(false);

            assertThatThrownBy(() -> addressService.getAddressesByUserId(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Address Tests")
    class UpdateAddressTests {

        @Test
        @DisplayName("Should update address successfully")
        void shouldUpdateAddressSuccessfully() {
            when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(testAddress));
            doNothing().when(userMapper).updateAddressFromRequest(any(), any());
            when(addressRepository.save(any(Address.class))).thenReturn(testAddress);
            when(userMapper.toAddressResponse(any(Address.class))).thenReturn(testAddressResponse);

            AddressResponse result = addressService.updateAddress(userId, addressId, addressRequest);

            assertThat(result).isNotNull();
            verify(addressRepository).save(any(Address.class));
        }

        @Test
        @DisplayName("Should throw when address not found")
        void shouldThrowWhenAddressNotFound() {
            when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> addressService.updateAddress(userId, addressId, addressRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Address Tests")
    class DeleteAddressTests {

        @Test
        @DisplayName("Should delete address successfully")
        void shouldDeleteAddressSuccessfully() {
            when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(testAddress));

            addressService.deleteAddress(userId, addressId);

            verify(addressRepository).delete(testAddress);
        }

        @Test
        @DisplayName("Should throw when address not found")
        void shouldThrowWhenAddressNotFound() {
            when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> addressService.deleteAddress(userId, addressId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Set Default Address Tests")
    class SetDefaultAddressTests {

        @Test
        @DisplayName("Should set default address successfully")
        void shouldSetDefaultAddressSuccessfully() {
            when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(testAddress));
            when(addressRepository.save(any(Address.class))).thenReturn(testAddress);
            when(userMapper.toAddressResponse(any(Address.class))).thenReturn(testAddressResponse);

            AddressResponse result = addressService.setDefaultAddress(userId, addressId);

            assertThat(result).isNotNull();
            verify(addressRepository).clearDefaultForUser(userId, addressId);
            assertThat(testAddress.getIsDefault()).isTrue();
        }
    }
}
