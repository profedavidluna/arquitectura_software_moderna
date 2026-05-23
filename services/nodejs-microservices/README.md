# Node.js/TypeScript Microservices - E-Commerce Platform

## Architecture Overview

This project implements a complete e-commerce platform using **8 microservices** built with Node.js 18 and TypeScript 5. It demonstrates key microservice patterns including Event-Driven Architecture, Saga Pattern, Circuit Breaker, and CQRS.

## Services

| Service | Port | Description |
|---------|------|-------------|
| user-service | 3082 | User management, profiles, addresses |
| product-service | 3083 | Product catalog, categories, search |
| cart-service | 3084 | Shopping cart, pricing, coupons |
| order-service | 3085 | Order lifecycle, Saga orchestration |
| payment-service | 3086 | Payment processing, refunds |
| inventory-service | 3087 | Stock management, reservations |
| notification-service | 3088 | Email notifications via Kafka |
| analytics-service | 3089 | Event aggregation, metrics |

## Technology Stack

- **Runtime**: Node.js 18 + TypeScript 5.3
- **Web Framework**: Express 4.18
- **Messaging**: KafkaJS 2.2.4
- **Database**: PostgreSQL (pg 8.11.3)
- **Cache**: Redis (ioredis 5.3.2)
- **Testing**: Jest 29.7 + ts-jest

## Infrastructure

- **Kafka** (port 9096): Event streaming between services
- **PostgreSQL** (port 5440): 7 isolated databases (one per stateful service)
- **Redis** (port 6380): Caching for cart and product services

## Architecture Patterns

### 1. Circuit Breaker
Prevents cascading failures when downstream services are unavailable. Implemented in cart-service (calls product-service) and order-service (calls multiple services).

### 2. Saga Pattern (Orchestration)
Order-service orchestrates the order creation flow:
1. Validate cart → 2. Reserve inventory → 3. Process payment → 4. Confirm order

If any step fails, compensating transactions are executed in reverse.

### 3. Event-Driven Architecture
Services communicate asynchronously via Kafka topics:
- `user-events`: User registration, profile updates
- `product-events`: Product creation, price changes
- `order-events`: Order created, confirmed, cancelled
- `payment-events`: Payment processed, refunded
- `inventory-events`: Stock reserved, released

### 4. Repository Pattern
Database access is abstracted behind repository interfaces, enabling testability and separation of concerns.

### 5. Service Layer Pattern
Business logic lives in service implementations that implement domain interfaces.

## Quick Start

```bash
# Start all infrastructure and services
docker-compose up -d

# Check service health
curl http://localhost:3082/health
curl http://localhost:3083/health
curl http://localhost:3084/health
curl http://localhost:3085/health
curl http://localhost:3086/health
curl http://localhost:3087/health
curl http://localhost:3088/health
curl http://localhost:3089/health
```

## Development

```bash
# Install dependencies for a service
cd user-service && npm install

# Run in development mode
npm run dev

# Run tests
npm test

# Build for production
npm run build
```

## API Examples

### Create User
```bash
curl -X POST http://localhost:3082/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","firstName":"John","lastName":"Doe","password":"secret123"}'
```

### Create Product
```bash
curl -X POST http://localhost:3083/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","description":"Gaming laptop","price":1299.99,"categoryId":"cat-1","stock":50}'
```

### Add to Cart
```bash
curl -X POST http://localhost:3084/api/v1/carts/{cartId}/items \
  -H "Content-Type: application/json" \
  -d '{"productId":"prod-1","quantity":2}'
```

### Create Order
```bash
curl -X POST http://localhost:3085/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-1","cartId":"cart-1","shippingAddressId":"addr-1"}'
```

## Project Structure

Each service follows Clean Architecture principles:
```
service-name/
├── package.json
├── tsconfig.json
├── Dockerfile
└── src/
    ├── index.ts              # Entry point, Express app setup
    ├── config/index.ts       # Environment configuration
    ├── domain/
    │   ├── models/           # Domain entities (pure TypeScript)
    │   └── interfaces/       # Service contracts
    ├── application/
    │   └── ServiceImpl.ts    # Business logic implementation
    ├── infrastructure/
    │   ├── persistence/      # Database repositories
    │   ├── messaging/        # Kafka producers/consumers
    │   ├── cache/            # Redis client (where applicable)
    │   └── web/              # Express controllers, DTOs
    └── tests/                # Unit tests
```

## Educational Notes

This project is designed for a Software Architecture course. Each service demonstrates:
- **Separation of Concerns**: Domain logic is isolated from infrastructure
- **Dependency Inversion**: High-level modules don't depend on low-level modules
- **Single Responsibility**: Each service owns its data and business rules
- **Eventual Consistency**: Services synchronize state through events
- **Resilience**: Circuit breakers and retries handle partial failures
