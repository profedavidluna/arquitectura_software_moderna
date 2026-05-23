package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.*;
import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.exception.DuplicateResourceException;
import com.ecommerce.userservice.exception.GlobalExceptionHandler;
import com.ecommerce.userservice.exception.ResourceNotFoundException;
import com.ecommerce.userservice.service.UserService;
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
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userId;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();

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
    }

    @Nested
    @DisplayName("POST /api/v1/users")
    class CreateUserEndpoint {

        @Test
        @DisplayName("Should create user and return 201")
        void shouldCreateUser() throws Exception {
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("john@example.com")
                    .username("john_doe")
                    .password("SecureP@ss123")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            when(userService.createUser(any(CreateUserRequest.class))).thenReturn(testUserResponse);

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.username").value("john_doe"));
        }

        @Test
        @DisplayName("Should return 400 for invalid input")
        void shouldReturn400ForInvalidInput() throws Exception {
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("invalid-email")
                    .username("ab")
                    .password("short")
                    .firstName("")
                    .lastName("")
                    .build();

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 for duplicate email")
        void shouldReturn409ForDuplicateEmail() throws Exception {
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("john@example.com")
                    .username("john_doe")
                    .password("SecureP@ss123")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            when(userService.createUser(any(CreateUserRequest.class)))
                    .thenThrow(new DuplicateResourceException("User", "email", "john@example.com"));

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class GetUserByIdEndpoint {

        @Test
        @DisplayName("Should return user by ID")
        void shouldReturnUserById() throws Exception {
            when(userService.getUserById(userId)).thenReturn(testUserResponse);

            mockMvc.perform(get("/api/v1/users/{id}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId.toString()))
                    .andExpect(jsonPath("$.email").value("john@example.com"));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(userService.getUserById(userId))
                    .thenThrow(new ResourceNotFoundException("User", "id", userId));

            mockMvc.perform(get("/api/v1/users/{id}", userId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users")
    class GetAllUsersEndpoint {

        @Test
        @DisplayName("Should return paginated users")
        void shouldReturnPaginatedUsers() throws Exception {
            PagedResponse<UserResponse> pagedResponse = PagedResponse.<UserResponse>builder()
                    .content(List.of(testUserResponse))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .last(true)
                    .build();

            when(userService.getAllUsers(any())).thenReturn(pagedResponse);

            mockMvc.perform(get("/api/v1/users")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{id}")
    class UpdateUserEndpoint {

        @Test
        @DisplayName("Should update user and return 200")
        void shouldUpdateUser() throws Exception {
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .firstName("Jane")
                    .build();

            when(userService.updateUser(eq(userId), any(UpdateUserRequest.class))).thenReturn(testUserResponse);

            mockMvc.perform(put("/api/v1/users/{id}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/{id}")
    class DeleteUserEndpoint {

        @Test
        @DisplayName("Should soft delete user and return 204")
        void shouldDeleteUser() throws Exception {
            doNothing().when(userService).deleteUser(userId);

            mockMvc.perform(delete("/api/v1/users/{id}", userId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("User", "id", userId))
                    .when(userService).deleteUser(userId);

            mockMvc.perform(delete("/api/v1/users/{id}", userId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/search")
    class SearchUsersEndpoint {

        @Test
        @DisplayName("Should search users")
        void shouldSearchUsers() throws Exception {
            PagedResponse<UserResponse> pagedResponse = PagedResponse.<UserResponse>builder()
                    .content(List.of(testUserResponse))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .last(true)
                    .build();

            when(userService.searchUsers(eq("john"), any())).thenReturn(pagedResponse);

            mockMvc.perform(get("/api/v1/users/search")
                            .param("q", "john"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }
}
