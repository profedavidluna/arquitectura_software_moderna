package com.ecommerce.userservice.integration;

import com.ecommerce.userservice.dto.CreateUserRequest;
import com.ecommerce.userservice.dto.UpdateUserRequest;
import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("user_db_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Should create user via REST API")
    void shouldCreateUser() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("integration@example.com")
                .username("integration_user")
                .password("SecureP@ss123")
                .firstName("Integration")
                .lastName("Test")
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("integration@example.com"))
                .andExpect(jsonPath("$.username").value("integration_user"))
                .andExpect(jsonPath("$.firstName").value("Integration"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("Should get user by ID")
    void shouldGetUserById() throws Exception {
        User user = createTestUser("get@example.com", "get_user");

        mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("get@example.com"))
                .andExpect(jsonPath("$.username").value("get_user"));
    }

    @Test
    @Order(3)
    @DisplayName("Should return 404 for non-existent user")
    void shouldReturn404ForNonExistentUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(4)
    @DisplayName("Should return paginated users")
    void shouldReturnPaginatedUsers() throws Exception {
        createTestUser("page1@example.com", "page_user1");
        createTestUser("page2@example.com", "page_user2");

        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @Order(5)
    @DisplayName("Should update user")
    void shouldUpdateUser() throws Exception {
        User user = createTestUser("update@example.com", "update_user");

        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .build();

        mockMvc.perform(put("/api/v1/users/{id}", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("Name"));
    }

    @Test
    @Order(6)
    @DisplayName("Should soft delete user")
    void shouldSoftDeleteUser() throws Exception {
        User user = createTestUser("delete@example.com", "delete_user");

        mockMvc.perform(delete("/api/v1/users/{id}", user.getId()))
                .andExpect(status().isNoContent());

        // Verify user is soft deleted (still exists but inactive)
        User deletedUser = userRepository.findById(user.getId()).orElseThrow();
        Assertions.assertFalse(deletedUser.getIsActive());
    }

    @Test
    @Order(7)
    @DisplayName("Should search users")
    void shouldSearchUsers() throws Exception {
        createTestUser("search@example.com", "search_user");
        createTestUser("other@example.com", "other_user");

        mockMvc.perform(get("/api/v1/users/search")
                        .param("q", "search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username").value("search_user"));
    }

    @Test
    @Order(8)
    @DisplayName("Should return 409 for duplicate email")
    void shouldReturn409ForDuplicateEmail() throws Exception {
        createTestUser("duplicate@example.com", "first_user");

        CreateUserRequest request = CreateUserRequest.builder()
                .email("duplicate@example.com")
                .username("second_user")
                .password("SecureP@ss123")
                .firstName("Second")
                .lastName("User")
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(9)
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

    private User createTestUser(String email, String username) {
        User user = User.builder()
                .email(email)
                .username(username)
                .passwordHash(passwordEncoder.encode("TestP@ss123"))
                .firstName("Test")
                .lastName("User")
                .role(Role.CUSTOMER)
                .isActive(true)
                .emailVerified(false)
                .build();
        return userRepository.save(user);
    }
}
