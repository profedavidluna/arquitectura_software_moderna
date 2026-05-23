package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(UUID id);

    UserResponse getUserByEmail(String email);

    UserResponse getUserByUsername(String username);

    PagedResponse<UserResponse> getAllUsers(Pageable pageable);

    PagedResponse<UserResponse> searchUsers(String query, Pageable pageable);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    void deleteUser(UUID id);
}
