# SOA Architecture - .NET 8 Implementation

## Overview

This project demonstrates a **Service-Oriented Architecture (SOA)** implementation using .NET 8 / ASP.NET Core with Apache Kafka as the Enterprise Service Bus (ESB).

Three independent services communicate asynchronously through Kafka events, each with its own PostgreSQL database following the **Database-per-Service** pattern.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Enterprise Service Bus (Kafka)                     │
│                                                                       │
│  Topics: product.created, order.created, stock.reserved,             │
│          stock.insufficient, order.confirmed, order.cancelled,        │
│          stock.released                                               │
└──────────┬──────────────────────┬──────────────────────┬────────────┘
           │                      │                      │
    ┌──────▼──────┐       ┌──────▼──────┐       ┌──────▼──────┐
    │   Product   │       │    Order    │       │  Inventory  │
    │   Service   │       │   Service   │       │   Service   │
    │  (port 6091)│       │  (port 6092)│       │  (port 6093)│
    └──────┬──────┘       └──────┬──────┘       └──────┬──────┘
           │                      │                      │
    ┌──────▼──────┐       ┌──────▼──────┐       ┌──────▼──────┐
    │  PostgreSQL │       │  PostgreSQL │       │  PostgreSQL │
    │ product_db  │       │  order_db   │       │inventory_db │
    └─────────────┘       └─────────────┘       └─────────────┘
```

## Saga Pattern - Order Creation Flow

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│ Order Service│         │    Kafka     │         │  Inventory   │
│  (Orchestr.) │         │    (ESB)     │         │   Service    │
└──────┬───────┘         └──────┬───────┘         └──────┬───────┘
       │                        │                        │
       │  1. order.created      │                        │
       │───────────────────────>│                        │
       │                        │  2. order.created      │
       │                        │───────────────────────>│
       │                        │                        │
       │                        │  3a. stock.reserved    │ (Happy Path)
       │                        │<───────────────────────│
       │  3a. stock.reserved    │                        │
       │<───────────────────────│                        │
       │                        │                        │
       │  4a. order.confirmed   │                        │
       │───────────────────────>│                        │
       │                        │                        │
       │                        │  3b. stock.insufficient│ (Compensation)
       │                        │<───────────────────────│
       │  3b. stock.insufficient│                        │
       │<───────────────────────│                        │
       │                        │                        │
       │  4b. order.cancelled   │                        │
       │───────────────────────>│                        │
       │                        │  4b. order.cancelled   │
       │                        │───────────────────────>│
       │                        │                        │
       │                        │  5b. stock.released    │
       │                        │<───────────────────────│
```

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Runtime | .NET 8 |
| Web Framework | ASP.NET Core (Controllers) |
| Messaging | Apache Kafka (Confluent.Kafka) |
| Database | PostgreSQL (Npgsql.EntityFrameworkCore) |
| ORM | Entity Framework Core 8 |
| Testing | xUnit + Moq |
| Containerization | Docker + Docker Compose |

## Services

### Product Service (Port 6091)
- CRUD operations for product catalog
- Publishes `product.created` events

### Order Service (Port 6092)
- Order management with Saga orchestration
- Publishes: `order.created`, `order.confirmed`, `order.cancelled`
- Consumes: `stock.reserved`, `stock.insufficient`

### Inventory Service (Port 6093)
- Stock/inventory management
- Publishes: `stock.reserved`, `stock.insufficient`, `stock.released`
- Consumes: `order.created`, `order.cancelled`

## Quick Start

### Prerequisites
- Docker and Docker Compose
- .NET 8 SDK (for local development)

### Run with Docker Compose

```bash
docker-compose up --build
```

### Run Locally (Development)

Each service uses InMemory database by default:

```bash
# Terminal 1 - Product Service
cd ProductService
dotnet run --urls "http://localhost:6091"

# Terminal 2 - Order Service
cd OrderService
dotnet run --urls "http://localhost:6092"

# Terminal 3 - Inventory Service
cd InventoryService
dotnet run --urls "http://localhost:6093"
```

## API Endpoints

### Product Service (http://localhost:6091)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/product | List all products |
| GET | /api/product/{id} | Get product by ID |
| POST | /api/product | Create a product |
| DELETE | /api/product/{id} | Delete a product |

### Order Service (http://localhost:6092)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/order | List all orders |
| GET | /api/order/{id} | Get order by ID |
| POST | /api/order | Create an order (initiates Saga) |

### Inventory Service (http://localhost:6093)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/inventory | List all inventory |
| GET | /api/inventory/{productId} | Get inventory by product ID |
| POST | /api/inventory | Create/update inventory |

## Testing the Saga Flow

```bash
# 1. Create a product
curl -X POST http://localhost:6091/api/product \
  -H "Content-Type: application/json" \
  -d '{"name": "Laptop", "description": "Gaming Laptop", "price": 1299.99, "category": "Electronics"}'

# 2. Add inventory for the product (use the product ID from step 1)
curl -X POST http://localhost:6093/api/inventory \
  -H "Content-Type: application/json" \
  -d '{"productId": "<PRODUCT_ID>", "productName": "Laptop", "quantity": 50}'

# 3. Create an order (triggers Saga)
curl -X POST http://localhost:6092/api/order \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "customerEmail": "john@example.com",
    "items": [{
      "productId": "<PRODUCT_ID>",
      "productName": "Laptop",
      "quantity": 2,
      "unitPrice": 1299.99
    }]
  }'

# 4. Check order status (should be CONFIRMED after Saga completes)
curl http://localhost:6092/api/order/<ORDER_ID>

# 5. Check inventory (reserved quantity should increase)
curl http://localhost:6093/api/inventory/<PRODUCT_ID>
```

## Running Tests

```bash
# Run tests for each service
cd ProductService && dotnet test
cd OrderService && dotnet test
cd InventoryService && dotnet test
```

## Design Patterns

| Pattern | Implementation |
|---------|---------------|
| **Service Layer** | Interface + Implementation with DI |
| **Repository** | EF Core DbContext abstraction |
| **Observer/Pub-Sub** | Kafka event publishing and consumption |
| **Saga** | Distributed transaction for order creation |
| **DTO** | Separate request/response models |
| **Factory** | Event creation in service implementations |

## SOLID Principles

- **SRP**: Each service class has one responsibility
- **OCP**: New event handlers can be added without modifying existing code
- **LSP**: Implementations are substitutable for their interfaces
- **ISP**: Focused interfaces (IProductService, IOrderService, IInventoryService)
- **DIP**: Controllers depend on interfaces, not implementations

## SOA Principles Demonstrated

1. **Service Autonomy**: Each service owns its data and logic
2. **Loose Coupling**: Services communicate via events, not direct calls
3. **Service Contract**: Well-defined interfaces and DTOs
4. **Service Abstraction**: Implementation details hidden behind interfaces
5. **Service Reusability**: REST APIs consumable by any client
6. **Service Composability**: Order Service composes Product + Inventory
7. **Service Statelessness**: Each request processed independently
8. **Service Discoverability**: Standardized REST endpoints

## Docker Compose Ports

| Service | External Port | Internal Port |
|---------|--------------|---------------|
| Zookeeper | 2184 | 2181 |
| Kafka | 9095 | 29095 |
| PostgreSQL | 5439 | 5432 |
| Product Service | 6091 | 6091 |
| Order Service | 6092 | 6092 |
| Inventory Service | 6093 | 6093 |
