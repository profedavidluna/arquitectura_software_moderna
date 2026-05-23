# Payment Service

E-Commerce Payment Processing Microservice built with Spring Boot 3.4.5 and Java 21.

## Overview

The Payment Service handles all payment-related operations including:
- Payment processing with gateway abstraction (Stripe, PayPal)
- Full and partial refund processing
- Payment method management (tokenized, PCI compliant)
- Retry logic for failed payments with exponential backoff
- Fraud detection (velocity checks, amount limits)
- Kafka event publishing for payment lifecycle events

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Payment Controller                   │
├─────────────────────────────────────────────────────┤
│                   Payment Service                    │
├──────────┬──────────┬──────────┬────────────────────┤
│  Fraud   │ Gateway  │  Retry   │  Event Publisher   │
│Detection │ Factory  │  Logic   │     (Kafka)        │
├──────────┴──────────┴──────────┴────────────────────┤
│              JPA Repositories                        │
├─────────────────────────────────────────────────────┤
│              PostgreSQL (payment_db)                  │
└─────────────────────────────────────────────────────┘
```

## Tech Stack

| Technology | Purpose |
|-----------|---------|
| Spring Boot 3.4.5 | Application framework |
| Java 21 | Runtime |
| Spring Data JPA | Data access |
| Spring Kafka | Event publishing |
| Spring Security + OAuth2 | Authentication |
| PostgreSQL | Database |
| Resilience4j | Circuit breaker, retry |
| OpenTelemetry | Distributed tracing |
| Micrometer + Prometheus | Metrics |
| MapStruct | Object mapping |
| TestContainers | Integration testing |
| JaCoCo | Code coverage (80% target) |
| Pact | Contract testing |

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/payments` | Process a payment |
| GET | `/api/v1/payments/{id}` | Get payment by ID |
| GET | `/api/v1/payments/order/{orderId}` | Get payments for order |
| POST | `/api/v1/payments/{id}/refund` | Process refund |
| POST | `/api/v1/payments/{id}/retry` | Retry failed payment |
| GET | `/api/v1/payments/user/{userId}` | Get user payment history |
| POST | `/api/v1/payments/methods` | Save payment method |
| GET | `/api/v1/payments/methods/user/{userId}` | Get user payment methods |

## Running Locally

### Prerequisites
- Java 21
- Maven 3.9+
- PostgreSQL 15+ (port 5437)
- Kafka (port 9092)
- Keycloak (port 8180)

### Start the service
```bash
# Development mode
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Or with Docker
docker build -t payment-service .
docker run -p 8084:8084 payment-service
```

### Run tests
```bash
# Unit tests
./mvnw test

# Integration tests (requires Docker for TestContainers)
./mvnw verify

# With coverage report
./mvnw verify jacoco:report
```

## Configuration

The service uses Spring profiles for environment-specific configuration:

| Profile | Description |
|---------|-------------|
| `default` | Production-ready settings |
| `dev` | Development with debug logging |
| `test` | H2 in-memory database, Kafka disabled |
| `prod` | Optimized for production |

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_USERNAME` | postgres | Database username |
| `DB_PASSWORD` | postgres | Database password |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka brokers |
| `KEYCLOAK_ISSUER_URI` | http://localhost:8180/realms/ecommerce | OAuth2 issuer |
| `KEYCLOAK_JWK_URI` | (derived from issuer) | JWK Set URI |

## Payment Processing Flow

```
1. Receive payment request
2. Idempotency check (prevent duplicates)
3. Fraud detection (velocity, amount limits)
4. Resolve payment token
5. Create transaction record (PROCESSING)
6. Call payment gateway
7. Update transaction (COMPLETED/FAILED)
8. Publish Kafka event
9. Return response
```

## Retry Logic

Failed payments can be retried up to 3 times with exponential backoff:
- Attempt 1: 1 second delay
- Attempt 2: 2 seconds delay
- Attempt 3: 4 seconds delay

## PCI Compliance

- **No raw card data stored** - only tokenized references
- **Last-four digits** stored for display purposes only
- **Sensitive data masked** in logs
- **Token-based** payment method storage

## Kafka Events

| Topic | Event | Description |
|-------|-------|-------------|
| `payment.processed` | PAYMENT_PROCESSED | Payment completed successfully |
| `payment.failed` | PAYMENT_FAILED | Payment processing failed |

## Monitoring

- **Health**: `GET /actuator/health`
- **Metrics**: `GET /actuator/prometheus`
- **API Docs**: `GET /swagger-ui.html`
- **Circuit Breakers**: `GET /actuator/circuitbreakers`

## Database Schema

The service uses the `payment_db` database with the following tables:
- `transactions` - Payment transaction records
- `refunds` - Refund records linked to transactions
- `payment_methods` - Tokenized payment methods
- `payment_retry_log` - Retry attempt history

Port: **5437**

## Project Structure

```
src/main/java/com/ecommerce/paymentservice/
├── PaymentServiceApplication.java
├── config/          # Security, Kafka, OpenAPI configuration
├── controller/      # REST controllers
├── dto/             # Request/Response DTOs
├── entity/          # JPA entities
│   └── enums/       # Status enumerations
├── event/           # Kafka event publishing
├── exception/       # Custom exceptions & global handler
├── gateway/         # Payment gateway abstraction
├── mapper/          # MapStruct mappers
├── repository/      # Spring Data JPA repositories
└── service/         # Business logic
```
