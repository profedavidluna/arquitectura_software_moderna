# Cart Service

E-Commerce Shopping Cart Microservice built with Spring Boot 3.4.5 and Java 21.

## Overview

The Cart Service manages shopping carts for the e-commerce platform. It handles cart creation, item management, coupon application, total calculation, and cart expiration.

## Architecture

- **Framework**: Spring Boot 3.4.5
- **Language**: Java 21
- **Database**: PostgreSQL (cart_db, port 5436)
- **Cache**: Redis (active cart caching with TTL)
- **Messaging**: Apache Kafka (cart events)
- **Security**: OAuth2 Resource Server (Keycloak)
- **Resilience**: Resilience4j (Circuit Breaker, Retry)
- **Documentation**: OpenAPI 3.0 / Swagger UI

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/carts` | Create a new cart |
| GET | `/api/v1/carts/{id}` | Get cart by ID |
| GET | `/api/v1/carts/user/{userId}` | Get active cart for user |
| POST | `/api/v1/carts/{id}/items` | Add item to cart |
| PUT | `/api/v1/carts/{id}/items/{itemId}` | Update item quantity |
| DELETE | `/api/v1/carts/{id}/items/{itemId}` | Remove item from cart |
| DELETE | `/api/v1/carts/{id}` | Clear cart |
| POST | `/api/v1/carts/{id}/coupon` | Apply coupon |
| DELETE | `/api/v1/carts/{id}/coupon` | Remove coupon |

## Features

### Cart Total Calculation
- **Subtotal**: Sum of all item prices × quantities
- **Tax**: Configurable rate (default 8%)
- **Shipping**: Free for orders over $50, otherwise $5.99
- **Discount**: Applied from coupon codes
- **Total**: subtotal + tax + shipping - discount

### Coupon System
- **Percentage coupons**: Codes ending with "PCT" (e.g., `SAVE10PCT` = 10% off)
- **Fixed amount coupons**: Codes ending with "OFF" (e.g., `SAVE10OFF` = $10 off)
- **Default**: 10% discount for other valid codes

### Cart Expiration
- Carts expire 30 days after creation
- Scheduled job runs hourly to mark expired carts as ABANDONED
- Expired carts cannot be modified

### Redis Caching
- Active carts cached with 15-minute TTL
- Cache evicted on cart modifications
- Reduces database load for frequently accessed carts

### Circuit Breaker (Product Service)
- Resilience4j circuit breaker for Product Service calls
- Automatic retry with exponential backoff
- Fallback behavior when Product Service is unavailable

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL (port 5436)
- Redis (port 6379)
- Apache Kafka (port 9092)

### Build and Run

```bash
# Build
./mvnw clean package

# Run with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
./mvnw test
```

### Docker

```bash
docker build -t cart-service .
docker run -p 8083:8083 cart-service
```

## Configuration

Key configuration properties in `application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `cart.tax-rate` | 0.08 | Tax rate (8%) |
| `cart.free-shipping-threshold` | 50.00 | Free shipping threshold |
| `cart.default-shipping-amount` | 5.99 | Default shipping cost |
| `cart.expiration-days` | 30 | Cart expiration in days |

## API Documentation

- Swagger UI: http://localhost:8083/swagger-ui.html
- OpenAPI JSON: http://localhost:8083/v3/api-docs

## Monitoring

- Health: http://localhost:8083/actuator/health
- Metrics: http://localhost:8083/actuator/metrics
- Prometheus: http://localhost:8083/actuator/prometheus
- Circuit Breakers: http://localhost:8083/actuator/circuitbreakers

## Database Schema

See `shared/database-schemas/cart-db-schema.sql` for the complete schema definition.

## Testing

```bash
# Unit tests
./mvnw test

# Integration tests (requires Docker for TestContainers)
./mvnw verify -P integration-test
```

## Project Structure

```
src/main/java/com/ecommerce/cartservice/
├── CartServiceApplication.java
├── client/                  # External service clients
├── config/                  # Configuration classes
├── controller/              # REST controllers
├── dto/                     # Data Transfer Objects
├── entity/                  # JPA entities
├── exception/               # Exception handling
├── mapper/                  # MapStruct mappers
├── repository/              # JPA repositories
├── scheduler/               # Scheduled tasks
└── service/                 # Business logic
```
