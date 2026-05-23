package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.AddressRequest;
import com.ecommerce.userservice.dto.AddressResponse;

import java.util.List;
import java.util.UUID;

public interface AddressService {

    AddressResponse createAddress(UUID userId, AddressRequest request);

    List<AddressResponse> getAddressesByUserId(UUID userId);

    AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request);

    void deleteAddress(UUID userId, UUID addressId);

    AddressResponse setDefaultAddress(UUID userId, UUID addressId);
}
