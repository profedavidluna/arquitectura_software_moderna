package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.AddressRequest;
import com.ecommerce.userservice.dto.AddressResponse;
import com.ecommerce.userservice.entity.Address;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.ResourceNotFoundException;
import com.ecommerce.userservice.mapper.UserMapper;
import com.ecommerce.userservice.repository.AddressRepository;
import com.ecommerce.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public AddressResponse createAddress(UUID userId, AddressRequest request) {
        log.info("Creating address for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Address address = userMapper.toAddressEntity(request);
        address.setUser(user);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultForUser(userId, UUID.randomUUID());
            address.setIsDefault(true);
        }

        Address savedAddress = addressRepository.save(address);
        log.info("Address created successfully with id: {} for user: {}", savedAddress.getId(), userId);

        return userMapper.toAddressResponse(savedAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddressesByUserId(UUID userId) {
        log.debug("Fetching addresses for user: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        return addressRepository.findByUserId(userId).stream()
                .map(userMapper::toAddressResponse)
                .toList();
    }

    @Override
    public AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request) {
        log.info("Updating address: {} for user: {}", addressId, userId);

        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        userMapper.updateAddressFromRequest(request, address);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultForUser(userId, addressId);
            address.setIsDefault(true);
        }

        Address updatedAddress = addressRepository.save(address);
        log.info("Address updated successfully: {} for user: {}", addressId, userId);

        return userMapper.toAddressResponse(updatedAddress);
    }

    @Override
    public void deleteAddress(UUID userId, UUID addressId) {
        log.info("Deleting address: {} for user: {}", addressId, userId);

        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        addressRepository.delete(address);
        log.info("Address deleted successfully: {} for user: {}", addressId, userId);
    }

    @Override
    public AddressResponse setDefaultAddress(UUID userId, UUID addressId) {
        log.info("Setting default address: {} for user: {}", addressId, userId);

        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        addressRepository.clearDefaultForUser(userId, addressId);
        address.setIsDefault(true);

        Address updatedAddress = addressRepository.save(address);
        log.info("Default address set successfully: {} for user: {}", addressId, userId);

        return userMapper.toAddressResponse(updatedAddress);
    }
}
