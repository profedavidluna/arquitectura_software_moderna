# Order Service

E-Commerce Order Management Microservice built with Spring Boot 3.4.5 and Java 21.

## Overview

The Order Service handles the complete order lifecycle including creation from cart, status management, cancellation, and event-driven communication with other services via Kafka.

## Architecture

- **Framework**: Spring Boot 3.4.5
- **Language**: Java 21
- **Database**: PostgreSQL (port 5434, database: order_db)
- **Messaging**: Apache Kafka
- **Security**: OAuth2 Resource Server (Keycloak)
- **Observability**: Micrometer + Prometheus + OpenTelemetry

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/orders` | Create order from cart |
| GET | `/api/v1/orders/{id}` | Get order by ID |
| GET | `/api/v1/orders` | List orders (paginated, filterable) |
| GET | `/api/v1/orders/user/{userId}` | Get user's orders |
| PUT | `/api/v1/orders/{id}/status` | Update order status |
| POST | `/api/v1/orders/{id}/cancel` | Cancel order |
| GET | `/api/v1/orders/{id}/history` | Get status history |

## Order Status Lifecycle

```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
    ↓         ↓
 CANCELLED  CANCELLED                          REFUNDED
```

### Valid Transitions
- PENDING → CONFIRMED, CANCELLED
- CONFIRMED → PROCESSING, CANCELLED
- PROCESSING → SHIPPED
- SHIPPED → DELIVERED
- DELIVERED → REFUNDED

## Saga Pattern (Choreography-Based)

The order creation follows a choreography-based saga:

1. **Order Created** → Publishes `order.created` event
2. **Payment Processed** → Listens for `payment.processed`, advances to CONFIRMED
3. **Inventory Reserved** → Listens for `inventory.reserved`, advances to PROCESSING
4. **Compensation** → On failure, cancels order and publishes `order.cancelled`

### Kafka Topics

**Published:**
- `order.created` - When a new order is placed
- `order.confirmed` - When payment is confirmed
- `order.shipped` - When order is shipped
- `order.cancelled` - When order is cancelled

**Consumed:**
- `payment.processed` - Payment service confirms/rejects payment
- `inventory.reserved` - Inventory service confirms/rejects stock reservation

## External Service Dependencies

| Service | Purpose | Circuit Breaker |
|---------|---------|-----------------|
| Cart Service | Fetch cart items for order creation | ✅ |
| Inventory Service | Reserve/release stock | ✅ |
| Payment Service | Process/refund payments | ✅ |

All external calls use Resilience4j circuit breakers and retry mechanisms.

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL (port 5434)
- Apache Kafka (port 9092)
- Keycloak (port 8180)

### Start the service
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Run tests
```bash
./mvnw test
```

### Run with Docker
```bash
docker build -t order-service .
docker run -p 8084:8084 order-service
```

## Configuration

Key configuration properties (see `application.yml`):

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8084 | Service port |
| `spring.datasource.url` | jdbc:postgresql://localhost:5434/order_db | Database URL |
| `spring.kafka.bootstrap-servers` | localhost:9092 | Kafka brokers |
| `services.cart-service.url` | http://localhost:8083 | Cart service URL |
| `services.inventory-service.url` | http://localhost:8085 | Inventory service URL |
| `services.payment-service.url` | http://localhost:8086 | Payment service URL |

## API Documentation

- Swagger UI: http://localhost:8084/swagger-ui.html
- OpenAPI JSON: http://localhost:8084/v3/api-docs

## Health & Monitoring

- Health: http://localhost:8084/actuator/health
- Metrics: http://localhost:8084/actuator/metrics
- Prometheus: http://localhost:8084/actuator/prometheus

## Testing

- **Unit Tests**: JUnit 5 + Mockito (target 80% coverage)
- **Integration Tests**: TestContainers (PostgreSQL)
- **Contract Tests**: Pact (consumer-driven)
- **Coverage**: JaCoCo with 80% line coverage threshold

## Project Structure

```
src/main/java/com/ecommerce/orderservice/
├── OrderServiceApplication.java
├── client/          # REST clients for external services
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/             # Request/Response DTOs
├── entity/          # JPA entities
├── event/           # Kafka event publishing/consuming
├── exception/       # Custom exceptions & global handler
├── mapper/          # MapStruct mappers
├── repository/      # Spring Data JPA repositories
└── service/         # Business logic
```
