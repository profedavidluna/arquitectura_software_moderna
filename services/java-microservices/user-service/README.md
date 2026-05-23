# User Service

E-Commerce User Management Microservice built with Spring Boot 3.4.5 and Java 21.

## Overview

The User Service manages user accounts, profiles, and addresses for the e-commerce platform. It provides RESTful APIs for user CRUD operations, address management, and user search functionality.

## Tech Stack

- **Java 21** - Runtime
- **Spring Boot 3.4.5** - Framework
- **Spring Data JPA** - Data access
- **PostgreSQL** - Database
- **Spring Security + OAuth2** - Authentication/Authorization
- **Spring Kafka** - Event publishing
- **MapStruct** - Object mapping
- **Lombok** - Boilerplate reduction
- **SpringDoc OpenAPI** - API documentation
- **Micrometer + Prometheus** - Metrics
- **OpenTelemetry** - Distributed tracing
- **TestContainers** - Integration testing
- **JaCoCo** - Code coverage

## API Endpoints

### Users

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/users` | Create a new user |
| GET | `/api/v1/users/{id}` | Get user by ID |
| GET | `/api/v1/users` | List users (paginated) |
| PUT | `/api/v1/users/{id}` | Update user |
| DELETE | `/api/v1/users/{id}` | Soft delete user |
| GET | `/api/v1/users/search?q=` | Search users |

### Addresses

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/users/{userId}/addresses` | Create address |
| GET | `/api/v1/users/{userId}/addresses` | List user addresses |
| PUT | `/api/v1/users/{userId}/addresses/{addressId}` | Update address |
| DELETE | `/api/v1/users/{userId}/addresses/{addressId}` | Delete address |
| PUT | `/api/v1/users/{userId}/addresses/{addressId}/default` | Set default address |

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+ (or Docker)
- Kafka (optional, for event publishing)

## Getting Started

### Local Development

1. **Start PostgreSQL** (via Docker):
   ```bash
   docker run -d --name user-db \
     -e POSTGRES_DB=user_db \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=postgres \
     -p 5432:5432 \
     postgres:15-alpine
   ```

2. **Build the project**:
   ```bash
   ./mvnw clean package -DskipTests
   ```

3. **Run the application**:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

4. **Access the API**:
   - API: http://localhost:8081/api/v1/users
   - Swagger UI: http://localhost:8081/swagger-ui.html
   - Health: http://localhost:8081/actuator/health
   - Metrics: http://localhost:8081/actuator/prometheus

### Using Docker

```bash
docker build -t user-service .
docker run -d --name user-service \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=5432 \
  -e DB_NAME=user_db \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -p 8081:8081 \
  user-service
```

### Using Docker Compose (from project root)

```bash
docker-compose up user-service
```

## Running Tests

### Unit Tests
```bash
./mvnw test
```

### Integration Tests (requires Docker for TestContainers)
```bash
./mvnw verify -Pintegration-tests
```

### Code Coverage Report
```bash
./mvnw test jacoco:report
# Report at: target/site/jacoco/index.html
```

## Configuration

The service uses Spring profiles for environment-specific configuration:

| Profile | Description |
|---------|-------------|
| `dev` | Local development (default) |
| `test` | Testing with H2 in-memory DB |
| `prod` | Production settings |

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | user_db | Database name |
| `DB_USERNAME` | postgres | Database username |
| `DB_PASSWORD` | postgres | Database password |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka brokers |
| `KEYCLOAK_ISSUER_URI` | http://localhost:8080/realms/ecommerce | Keycloak issuer |

## Project Structure

```
src/
├── main/
│   ├── java/com/ecommerce/userservice/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── entity/          # JPA entities
│   │   ├── event/           # Kafka event publishing
│   │   ├── exception/       # Exception handling
│   │   ├── mapper/          # MapStruct mappers
│   │   ├── repository/      # Spring Data repositories
│   │   └── service/         # Business logic
│   └── resources/
│       └── application.yml  # Configuration
└── test/
    └── java/com/ecommerce/userservice/
        ├── controller/      # Controller unit tests
        ├── integration/     # Integration tests
        └── service/         # Service unit tests
```

## Monitoring

- **Health Check**: `GET /actuator/health`
- **Prometheus Metrics**: `GET /actuator/prometheus`
- **Info**: `GET /actuator/info`

## Kafka Events

The service publishes the following events:

| Topic | Event | Description |
|-------|-------|-------------|
| `user.registered` | UserEvent | Published when a new user registers |
| `user.updated` | UserEvent | Published when user profile is updated |

## Security

- OAuth2 Resource Server with JWT validation
- BCrypt password hashing
- Role-based access control (ADMIN, CUSTOMER, SUPPORT)
- Stateless session management
- CSRF disabled (API-only service)
