# Product Service

E-Commerce Product Catalog Microservice built with Spring Boot 3.2+.

## Overview

The Product Service manages the product catalog, categories, and customer reviews. It provides RESTful APIs for CRUD operations, full-text search, filtering, and pagination.

## Tech Stack

- **Java 21** + **Spring Boot 3.4.5**
- **Spring Data JPA** - Data access with PostgreSQL
- **Spring Security** - OAuth2 Resource Server (Keycloak)
- **Spring Cache** - Caffeine in-memory caching
- **Spring Kafka** - Event publishing
- **PostgreSQL 16** - Primary database
- **MapStruct** - Object mapping
- **OpenAPI 3.0** - API documentation (Swagger UI)
- **Micrometer + Prometheus** - Metrics
- **OpenTelemetry** - Distributed tracing
- **TestContainers** - Integration testing

## Project Structure

```
src/main/java/com/ecommerce/productservice/
├── ProductServiceApplication.java
├── config/
│   ├── CacheConfig.java
│   ├── KafkaConfig.java
│   ├── OpenApiConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── ProductController.java
│   ├── CategoryController.java
│   └── ReviewController.java
├── dto/
│   ├── ProductRequest.java
│   ├── ProductResponse.java
│   ├── CategoryRequest.java
│   ├── CategoryResponse.java
│   ├── ReviewRequest.java
│   ├── ReviewResponse.java
│   └── PagedResponse.java
├── entity/
│   ├── Product.java
│   ├── Category.java
│   └── ProductReview.java
├── exception/
│   ├── ErrorResponse.java
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── DuplicateResourceException.java
├── mapper/
│   └── ProductMapper.java
├── repository/
│   ├── ProductRepository.java
│   ├── CategoryRepository.java
│   ├── ProductReviewRepository.java
│   └── ProductSpecification.java
└── service/
    ├── ProductService.java
    ├── ProductServiceImpl.java
    ├── CategoryService.java
    ├── CategoryServiceImpl.java
    ├── ReviewService.java
    └── ReviewServiceImpl.java
```

## API Endpoints

### Products

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/products` | Create a new product |
| GET | `/api/v1/products/{id}` | Get product by ID |
| GET | `/api/v1/products` | List products (paginated, filterable) |
| PUT | `/api/v1/products/{id}` | Update product |
| DELETE | `/api/v1/products/{id}` | Soft delete product |
| GET | `/api/v1/products/search?q=` | Full-text search |
| GET | `/api/v1/products/category/{categoryId}` | Products by category |
| GET | `/api/v1/products/featured` | Featured products |

### Categories

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/categories` | Create category |
| GET | `/api/v1/categories` | List categories (hierarchical) |
| GET | `/api/v1/categories/{id}` | Get category by ID |
| PUT | `/api/v1/categories/{id}` | Update category |
| DELETE | `/api/v1/categories/{id}` | Soft delete category |

### Reviews

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/products/{id}/reviews` | Add review |
| GET | `/api/v1/products/{id}/reviews` | Get product reviews |

## Query Parameters

### Pagination & Sorting
- `page` - Page number (default: 0)
- `size` - Page size (default: 20)
- `sortBy` - Sort field (default: createdAt)
- `sortDir` - Sort direction: asc/desc (default: desc)

### Filtering
- `categoryId` - Filter by category UUID
- `minPrice` - Minimum price
- `maxPrice` - Maximum price
- `featured` - Filter featured products (true/false)
- `tag` - Filter by tag

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 16 (or Docker)
- Kafka (optional, for event publishing)
- Keycloak (optional, for OAuth2)

## Running Locally

### 1. Start dependencies (Docker)

```bash
cd ../../shared
docker-compose up -d postgres-product kafka keycloak
```

### 2. Run the service

```bash
# Development profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Or with Maven wrapper
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. Access the API

- API: http://localhost:8082/api/v1/products
- Swagger UI: http://localhost:8082/swagger-ui.html
- Health: http://localhost:8082/actuator/health
- Metrics: http://localhost:8082/actuator/prometheus

## Running Tests

```bash
# Unit tests
./mvnw test

# Integration tests (requires Docker for TestContainers)
./mvnw verify -P integration-test

# With coverage report
./mvnw test jacoco:report
# Report at: target/site/jacoco/index.html
```

## Building

```bash
# Build JAR
./mvnw clean package -DskipTests

# Build Docker image
docker build -t product-service:latest .
```

## Docker

```bash
# Run with Docker
docker run -p 8082:8082 \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e KEYCLOAK_ISSUER_URI=http://keycloak:8180/realms/ecommerce \
  product-service:latest
```

## Configuration

Key configuration in `application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8082 | Service port |
| `spring.datasource.url` | jdbc:postgresql://localhost:5433/product_db | Database URL |
| `spring.kafka.bootstrap-servers` | localhost:9092 | Kafka brokers |
| `management.tracing.sampling.probability` | 0.1 | Trace sampling rate |

## Profiles

- `dev` - Development (DDL auto-update, debug logging)
- `test` - Testing (H2 in-memory, security disabled)
- `prod` - Production (DDL validate, minimal logging)

## Caching

Products are cached using Caffeine with:
- Max 500 entries
- 10-minute TTL
- Cache eviction on update/delete

## Security

- Public: GET endpoints (product listing, search, categories)
- Authenticated: POST reviews
- ADMIN role: Create/Update/Delete products and categories

## Monitoring

- Health check: `/actuator/health`
- Prometheus metrics: `/actuator/prometheus`
- OpenTelemetry tracing with configurable sampling rate
