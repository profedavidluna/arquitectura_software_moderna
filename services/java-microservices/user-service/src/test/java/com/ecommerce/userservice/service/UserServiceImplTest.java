package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.*;
import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.DuplicateResourceException;
import com.ecommerce.userservice.exception.ResourceNotFoundException;
import com.ecommerce.userservice.mapper.UserMapper;
import com.ecommerce.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserResponse testUserResponse;
    private CreateUserRequest createRequest;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("john@example.com")
                .username("john_doe")
                .passwordHash("hashed_password")
                .firstName("John")
                .lastName("Doe")
                .role(Role.CUSTOMER)
                .isActive(true)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testUserResponse = UserResponse.builder()
                .id(userId)
                .email("john@example.com")
                .username("john_doe")
                .firstName("John")
                .lastName("Doe")
                .role(Role.CUSTOMER)
                .isActive(true)
                .emailVerified(false)
                .build();

        createRequest = CreateUserRequest.builder()
                .email("john@example.com")
                .username("john_doe")
                .password("SecureP@ss123")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully")
        void shouldCreateUserSuccessfully() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userMapper.toEntity(any(CreateUserRequest.class))).thenReturn(testUser);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toResponse(any(User.class))).thenReturn(testUserResponse);

            UserResponse result = userService.createUser(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("john@example.com");
            assertThat(result.getUsername()).isEqualTo("john_doe");
            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode("SecureP@ss123");
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when email exists")
        void shouldThrowWhenEmailExists() {
            when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email");
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when username exists")
        void shouldThrowWhenUsernameExists() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername("john_doe")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("username");
        }
    }

    @Nested
    @DisplayName("Get User Tests")
    class GetUserTests {

        @Test
        @DisplayName("Should get user by ID")
        void shouldGetUserById() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            UserResponse result = userService.getUserById(userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found by ID")
        void shouldThrowWhenUserNotFoundById() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should get user by email")
        void shouldGetUserByEmail() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            UserResponse result = userService.getUserByEmail("john@example.com");

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Should get user by username")
        void shouldGetUserByUsername() {
            when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            UserResponse result = userService.getUserByUsername("john_doe");

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("john_doe");
        }
    }

    @Nested
    @DisplayName("List and Search Users Tests")
    class ListAndSearchTests {

        @Test
        @DisplayName("Should return paginated users")
        void shouldReturnPaginatedUsers() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);

            when(userRepository.findByIsActiveTrue(pageable)).thenReturn(userPage);
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            PagedResponse<UserResponse> result = userService.getAllUsers(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should search users by query")
        void shouldSearchUsers() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);

            when(userRepository.searchUsers("john", pageable)).thenReturn(userPage);
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            PagedResponse<UserResponse> result = userService.searchUsers("john", pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user successfully")
        void shouldUpdateUserSuccessfully() {
            UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            doNothing().when(userMapper).updateUserFromRequest(any(), any());
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toResponse(any(User.class))).thenReturn(testUserResponse);

            UserResponse result = userService.updateUser(userId, updateRequest);

            assertThat(result).isNotNull();
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw when updating with duplicate email")
        void shouldThrowWhenUpdatingWithDuplicateEmail() {
            UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                    .email("existing@example.com")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(userId, updateRequest))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent user")
        void shouldThrowWhenUpdatingNonExistentUser() {
            UpdateUserRequest updateRequest = UpdateUserRequest.builder().build();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(userId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should soft delete user")
        void shouldSoftDeleteUser() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.deleteUser(userId);

            assertThat(testUser.getIsActive()).isFalse();
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent user")
        void shouldThrowWhenDeletingNonExistentUser() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
