# E-Commerce Microservices Platform

A production-ready e-commerce platform built with **Spring Boot 3.4**, **Java 21**, and a microservices architecture. The system demonstrates enterprise-grade patterns including event-driven communication, distributed transactions, and comprehensive observability.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              API Gateway / Client                            │
└─────────────────────────────┬───────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────────┐
│ User Service │   │Product Service│   │   Cart Service   │
│   (8082)     │   │   (8083)     │   │     (8084)       │
└──────┬───────┘   └──────┬───────┘   └────────┬─────────┘
       │                  │                     │
       │    ┌─────────────┼─────────────────────┼──────────┐
       │    │             │                     │          │
       ▼    ▼             ▼                     ▼          ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────────┐
│Order Service │   │Payment Service│   │Inventory Service │
│   (8085)     │   │   (8086)     │   │     (8087)       │
└──────┬───────┘   └──────┬───────┘   └────────┬─────────┘
       │                  │                     │
       └──────────────────┼─────────────────────┘
                          │
              ┌───────────┼───────────┐
              ▼                       ▼
┌──────────────────┐       ┌──────────────────┐
│Notification Svc  │       │Analytics Service │
│     (8088)       │       │     (8089)       │
└──────────────────┘       └──────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                           Infrastructure Layer                                │
├──────────┬──────────┬──────────┬──────────────┬─────────────────────────────┤
│  Kafka   │PostgreSQL│  Redis   │   Keycloak   │  Prometheus/Grafana/Jaeger  │
│  (9092)  │  (5432)  │  (6379)  │   (8180)     │                             │
└──────────┴──────────┴──────────┴──────────────┴─────────────────────────────┘
```

## Microservices

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| **user-service** | 8082 | user_db | User registration, authentication, profile management, addresses |
| **product-service** | 8083 | product_db | Product catalog, categories, reviews, search |
| **cart-service** | 8084 | cart_db | Shopping cart management, pricing calculations, coupons |
| **order-service** | 8085 | order_db | Order lifecycle, Saga orchestration, status tracking |
| **payment-service** | 8086 | payment_db | Payment processing, refunds, multi-gateway support |
| **inventory-service** | 8087 | inventory_db | Stock management, reservations, low-stock alerts |
| **notification-service** | 8088 | — | Email notifications via Kafka event consumption |
| **analytics-service** | 8089 | analytics_db | Event aggregation, metrics, dashboards, reporting |

## Communication Patterns

### Synchronous (REST)
- **Cart → Product**: Validate product availability and pricing
- **Order → Cart**: Retrieve cart for checkout
- **Order → Inventory**: Reserve stock during order creation
- **Order → Payment**: Process payment for orders

### Asynchronous (Apache Kafka)
Event-driven communication for decoupled, eventually consistent operations:

```
┌──────────────┐     user-events      ┌──────────────────┐
│ User Service │ ──────────────────▶  │ Analytics Service │
└──────────────┘                      └──────────────────┘

┌──────────────┐    product-events    ┌──────────────────┐
│Product Service│ ──────────────────▶ │ Analytics Service │
└──────────────┘                      └──────────────────┘

┌──────────────┐     order-events     ┌──────────────────┐
│ Order Service│ ──────────────────▶  │Notification Svc  │
│              │ ──────────────────▶  │Inventory Service │
│              │ ──────────────────▶  │Analytics Service │
└──────────────┘                      └──────────────────┘

┌──────────────┐    payment-events    ┌──────────────────┐
│Payment Service│ ──────────────────▶ │ Order Service    │
│              │ ──────────────────▶  │Notification Svc  │
│              │ ──────────────────▶  │Analytics Service │
└──────────────┘                      └──────────────────┘

┌──────────────┐   inventory-events   ┌──────────────────┐
│Inventory Svc │ ──────────────────▶  │ Order Service    │
│              │ ──────────────────▶  │Analytics Service │
└──────────────┘                      └──────────────────┘
```

### Kafka Topics

| Topic | Producer | Consumers |
|-------|----------|-----------|
| `user-events` | user-service | analytics-service |
| `product-events` | product-service | analytics-service |
| `order-events` | order-service | notification-service, inventory-service, analytics-service |
| `payment-events` | payment-service | order-service, notification-service, analytics-service |
| `inventory-events` | inventory-service | order-service, analytics-service |

## Design Patterns

### Database per Service
Each microservice owns its database, ensuring loose coupling and independent deployability.

### Saga Pattern (Orchestration)
The **order-service** orchestrates the order creation saga:
1. Validate cart → 2. Reserve inventory → 3. Process payment → 4. Confirm order

Compensating transactions handle failures (e.g., release stock if payment fails).

### Circuit Breaker (Resilience4j)
Applied to all inter-service REST calls:
- **Sliding window**: 10 calls
- **Failure threshold**: 50%
- **Wait in open state**: 30s
- **Half-open calls**: 3

### CQRS (Command Query Responsibility Segregation)
The **analytics-service** implements CQRS by consuming events (commands/writes) from Kafka and serving aggregated read models via REST.

### Event Sourcing
Domain events are published to Kafka for every state change, enabling full audit trails and event replay.

### API Gateway Pattern
Services expose RESTful APIs with OpenAPI documentation. Each service is independently addressable.

## Running with Docker Compose

### Prerequisites
- Docker Engine 24+
- Docker Compose v2+
- 8 GB RAM minimum (recommended: 16 GB)

### Quick Start

```bash
# Clone and navigate to the project
cd services/java-microservices

# Build and start all services
docker compose up --build -d

# Watch logs
docker compose logs -f

# Check service health
docker compose ps
```

### Start Infrastructure Only

```bash
docker compose up -d zookeeper kafka postgres redis keycloak
```

### Start Individual Services

```bash
docker compose up -d user-service product-service
```

### Stop Everything

```bash
docker compose down

# Remove volumes (clean slate)
docker compose down -v
```

### Verify Services

```bash
# Check all health endpoints
curl http://localhost:8082/actuator/health  # user-service
curl http://localhost:8083/actuator/health  # product-service
curl http://localhost:8084/actuator/health  # cart-service
curl http://localhost:8085/actuator/health  # order-service
curl http://localhost:8086/actuator/health  # payment-service
curl http://localhost:8087/actuator/health  # inventory-service
curl http://localhost:8088/actuator/health  # notification-service
curl http://localhost:8089/actuator/health  # analytics-service
```

## API Endpoints

### User Service (8082)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/users` | Create user |
| GET | `/api/v1/users/{id}` | Get user by ID |
| GET | `/api/v1/users` | List users (paginated) |
| PUT | `/api/v1/users/{id}` | Update user |
| DELETE | `/api/v1/users/{id}` | Delete user |
| POST | `/api/v1/users/{id}/addresses` | Add address |
| GET | `/api/v1/users/{id}/addresses` | List addresses |
| PUT | `/api/v1/users/{id}/addresses/{addressId}` | Update address |
| DELETE | `/api/v1/users/{id}/addresses/{addressId}` | Delete address |

### Product Service (8083)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/products` | Create product |
| GET | `/api/v1/products/{id}` | Get product |
| GET | `/api/v1/products` | List/search products |
| PUT | `/api/v1/products/{id}` | Update product |
| DELETE | `/api/v1/products/{id}` | Delete product |
| POST | `/api/v1/categories` | Create category |
| GET | `/api/v1/categories` | List categories |
| POST | `/api/v1/products/{id}/reviews` | Add review |
| GET | `/api/v1/products/{id}/reviews` | List reviews |

### Cart Service (8084)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/carts` | Create cart |
| GET | `/api/v1/carts/{id}` | Get cart |
| POST | `/api/v1/carts/{id}/items` | Add item to cart |
| PUT | `/api/v1/carts/{id}/items/{itemId}` | Update item quantity |
| DELETE | `/api/v1/carts/{id}/items/{itemId}` | Remove item |
| POST | `/api/v1/carts/{id}/coupon` | Apply coupon |
| DELETE | `/api/v1/carts/{id}/coupon` | Remove coupon |
| DELETE | `/api/v1/carts/{id}` | Clear cart |

### Order Service (8085)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/orders` | Create order (from cart) |
| GET | `/api/v1/orders/{id}` | Get order |
| GET | `/api/v1/orders` | List user orders |
| PUT | `/api/v1/orders/{id}/status` | Update order status |
| POST | `/api/v1/orders/{id}/cancel` | Cancel order |
| GET | `/api/v1/orders/{id}/history` | Get status history |

### Payment Service (8086)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/payments/process` | Process payment |
| GET | `/api/v1/payments/{id}` | Get transaction |
| GET | `/api/v1/payments/order/{orderId}` | Get by order |
| POST | `/api/v1/payments/{id}/refund` | Refund payment |
| POST | `/api/v1/payments/methods` | Save payment method |
| GET | `/api/v1/payments/methods` | List payment methods |

### Inventory Service (8087)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/inventory` | Create inventory record |
| GET | `/api/v1/inventory/{productId}` | Get stock info |
| PUT | `/api/v1/inventory/{productId}` | Update inventory |
| POST | `/api/v1/inventory/{productId}/reserve` | Reserve stock |
| POST | `/api/v1/inventory/{productId}/release` | Release reservation |
| POST | `/api/v1/inventory/{productId}/deplete` | Deplete stock |
| POST | `/api/v1/inventory/{productId}/restock` | Restock |
| GET | `/api/v1/inventory/alerts` | Low stock alerts |

### Notification Service (8088)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check |

> Notification service is event-driven. It consumes Kafka events and sends emails automatically.

### Analytics Service (8089)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/analytics/dashboard` | Dashboard summary |
| GET | `/api/v1/analytics/revenue` | Revenue report |
| GET | `/api/v1/analytics/top-products` | Top selling products |
| GET | `/api/v1/analytics/user-activity` | User activity metrics |
| GET | `/api/v1/analytics/events` | Query events |

### Swagger UI

Each service exposes OpenAPI documentation:
```
http://localhost:{port}/swagger-ui.html
```

## Resilience Patterns

### Circuit Breaker
Prevents cascading failures when downstream services are unavailable.

```yaml
resilience4j:
  circuitbreaker:
    instances:
      serviceName:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
```

### Retry with Exponential Backoff
Automatically retries transient failures with increasing delays.

```yaml
resilience4j:
  retry:
    instances:
      serviceName:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
```

### Timeout
Prevents indefinite waiting for slow responses.

### Bulkhead
Isolates failures by limiting concurrent calls to downstream services.

## Monitoring & Observability

### Prometheus Metrics
Each service exposes metrics at `/actuator/prometheus`:
- JVM metrics (memory, GC, threads)
- HTTP request metrics (latency, error rates)
- Kafka consumer lag
- Circuit breaker state
- Custom business metrics

### Distributed Tracing (OpenTelemetry / Jaeger)
Trace requests across service boundaries with correlation IDs:
- Trace propagation via HTTP headers
- Span creation for REST calls and Kafka messages
- Configurable sampling rate (10% dev, 5% prod)

### Logging
Structured logging with trace correlation:
```
2024-01-15 10:30:45 [http-nio-8082-exec-1] [abc123def456] INFO  UserService - User created: user@email.com
```

### Health Checks
Spring Boot Actuator health endpoints with dependency checks:
- Database connectivity
- Kafka broker availability
- Redis connection
- Circuit breaker state

## Security

### Keycloak (OAuth2 / OpenID Connect)
- **Authorization Server**: Keycloak at port 8180
- **Realm**: `ecommerce`
- **Protocol**: OpenID Connect
- **Token Type**: JWT (RS256)

### Service Security
- All services act as OAuth2 Resource Servers
- JWT validation against Keycloak's JWKS endpoint
- Role-based access control (RBAC)
- Stateless authentication

### Access Flow
```
Client → Keycloak (authenticate) → JWT Token
Client → Service (JWT in Authorization header) → Validated → Response
```

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.5 |
| Messaging | Apache Kafka |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Security | Keycloak 24 (OAuth2/OIDC) |
| Resilience | Resilience4j |
| API Docs | SpringDoc OpenAPI |
| Mapping | MapStruct + Lombok |
| Testing | JUnit 5, Testcontainers, H2 |
| Observability | Micrometer, Prometheus, OpenTelemetry |
| Containerization | Docker, Docker Compose |

## Project Structure

```
java-microservices/
├── user-service/           # User management
├── product-service/        # Product catalog
├── cart-service/           # Shopping cart
├── order-service/          # Order processing
├── payment-service/        # Payment processing
├── inventory-service/      # Stock management
├── notification-service/   # Email notifications
├── analytics-service/      # Analytics & reporting
├── database/
│   └── init.sql           # Database initialization
├── docker-compose.yml      # Full orchestration
└── README.md              # This file
```

## Development

### Local Development (without Docker)

1. Start infrastructure:
```bash
docker compose up -d zookeeper kafka postgres redis keycloak
```

2. Run individual services with IDE or Maven:
```bash
cd user-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Running Tests

```bash
cd <service-name>
./mvnw test
```

Integration tests use **Testcontainers** to spin up PostgreSQL and Kafka automatically.

## License

This project is part of the Software Architecture course (2026).
