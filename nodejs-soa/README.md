# SOA (Service-Oriented Architecture) - Node.js/TypeScript Implementation

## 📋 Overview

This project demonstrates a **Service-Oriented Architecture (SOA)** implementation using Node.js, TypeScript, Apache Kafka as the Enterprise Service Bus (ESB), and PostgreSQL for persistence.

### What is SOA?

SOA is an architectural style where application components provide services to other components via a communications protocol over a network. Key principles:

- **Service Autonomy**: Each service operates independently
- **Loose Coupling**: Services interact through well-defined interfaces
- **Service Abstraction**: Internal implementation is hidden
- **Service Reusability**: Services can be reused across different processes
- **Service Composability**: Services can be composed to form larger processes
- **Service Discoverability**: Services can be discovered and invoked

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENT APPLICATIONS                           │
└──────────┬──────────────────────┬──────────────────────┬────────────┘
           │                      │                      │
           ▼                      ▼                      ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  Product Service │  │  Order Service   │  │ Inventory Service│
│    (port 4091)   │  │   (port 4092)    │  │   (port 4093)    │
│                  │  │                  │  │                  │
│  ┌────────────┐  │  │  ┌────────────┐  │  │  ┌────────────┐  │
│  │  Express   │  │  │  │  Express   │  │  │  │  Express   │  │
│  │    API     │  │  │  │    API     │  │  │  │    API     │  │
│  └────────────┘  │  │  └────────────┘  │  │  └────────────┘  │
│  ┌────────────┐  │  │  ┌────────────┐  │  │  ┌────────────┐  │
│  │  Service   │  │  │  │  Service   │  │  │  │  Service   │  │
│  │   Layer    │  │  │  │   Layer    │  │  │  │   Layer    │  │
│  └────────────┘  │  │  └────────────┘  │  │  └────────────┘  │
│  ┌────────────┐  │  │  ┌────────────┐  │  │  ┌────────────┐  │
│  │ Repository │  │  │  │ Repository │  │  │  │ Repository │  │
│  └────────────┘  │  │  └────────────┘  │  │  └────────────┘  │
└────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
         │                     │                      │
         │    ┌────────────────┴──────────────────┐   │
         │    │     APACHE KAFKA (ESB)            │   │
         │    │     Enterprise Service Bus        │   │
         │    │                                   │   │
         │    │  Topics:                          │   │
         │    │  • order.created                  │   │
         │    │  • stock.reserved                 │   │
         │    │  • stock.insufficient             │   │
         │    │  • order.confirmed                │   │
         │    │  • order.cancelled                │   │
         │    │  • stock.released                 │   │
         │    │  • product.created                │   │
         │    └───────────────────────────────────┘   │
         │                                            │
         ▼                                            ▼
┌──────────────────────────────────────────────────────────────┐
│                    POSTGRESQL (port 5437)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │  product_db  │  │   order_db   │  │ inventory_db │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└──────────────────────────────────────────────────────────────┘
```

## 🔄 Saga Pattern - Order Flow

The system implements the **Saga Pattern** for distributed transactions:

```
1. Client creates order → Order Service
2. Order Service publishes "order.created" event
3. Inventory Service consumes "order.created"
4. Inventory Service checks stock availability:
   
   ✅ Stock Available:
   ├── Reserve stock (quantity_reserved += quantity)
   ├── Publish "stock.reserved" event
   └── Order Service updates order to CONFIRMED
   
   ❌ Stock Insufficient:
   ├── Publish "stock.insufficient" event
   └── Order Service updates order to CANCELLED
   
5. If order is cancelled later:
   ├── Order Service publishes "order.cancelled"
   ├── Inventory Service releases reserved stock
   └── Inventory Service publishes "stock.released"
```

### Compensating Transactions

In the Saga pattern, if a step fails, compensating transactions undo previous steps:
- `stock.reserved` → compensated by `stock.released` (if order cancelled)
- `order.created` → compensated by order status change to CANCELLED

## 🛠️ Technology Stack

| Technology | Purpose |
|-----------|---------|
| Node.js 18 | Runtime environment |
| TypeScript 5 | Type-safe development |
| Express 4.18 | HTTP framework |
| KafkaJS | Kafka client for Node.js |
| pg | PostgreSQL client |
| Jest | Testing framework |
| Docker | Containerization |

## 🚀 How to Run

### Prerequisites
- Docker and Docker Compose installed
- Node.js 18+ (for local development)

### Start All Services

```bash
# Build and start all containers
docker-compose up --build

# Or run in detached mode
docker-compose up --build -d
```

### Local Development (without Docker)

```bash
# Start infrastructure only
docker-compose up zookeeper kafka postgres -d

# In separate terminals, start each service:
cd product-service && npm install && npm run dev
cd order-service && npm install && npm run dev
cd inventory-service && npm install && npm run dev
```

### Run Tests

```bash
cd product-service && npm test
cd order-service && npm test
cd inventory-service && npm test
```

## 📡 API Endpoints

### Product Service (http://localhost:4091)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/products | List all products |
| GET | /api/products/:id | Get product by ID |
| POST | /api/products | Create a product |
| PUT | /api/products/:id | Update a product |
| DELETE | /api/products/:id | Delete a product |
| GET | /health | Health check |

### Order Service (http://localhost:4092)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/orders | List all orders |
| GET | /api/orders/:id | Get order by ID |
| POST | /api/orders | Create an order |
| PUT | /api/orders/:id/cancel | Cancel an order |
| GET | /api/orders/user/:userId | Get orders by user |
| GET | /health | Health check |

### Inventory Service (http://localhost:4093)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/inventory | List all inventory |
| GET | /api/inventory/:productId | Get inventory by product |
| POST | /api/inventory | Create inventory entry |
| PUT | /api/inventory/:productId/stock | Update stock level |
| GET | /health | Health check |

## 📝 Example Usage

### Create a Product
```bash
curl -X POST http://localhost:4091/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gaming Keyboard",
    "description": "Mechanical RGB keyboard",
    "price": 89.99,
    "category": "Peripherals",
    "sku": "KB-GAME-01"
  }'
```

### Add Inventory
```bash
curl -X POST http://localhost:4093/api/inventory \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "productName": "Laptop Pro 15",
    "quantityAvailable": 100
  }'
```

### Create an Order (triggers Saga)
```bash
curl -X POST http://localhost:4092/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "items": [
      {
        "productId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "productName": "Laptop Pro 15",
        "quantity": 2,
        "unitPrice": 1299.99
      }
    ]
  }'
```

## 🏛️ SOLID Principles Applied

- **SRP (Single Responsibility)**: Each service class has one reason to change
- **OCP (Open/Closed)**: New event handlers can be added without modifying existing code
- **LSP (Liskov Substitution)**: Service implementations are substitutable for their interfaces
- **ISP (Interface Segregation)**: Focused interfaces per service domain
- **DIP (Dependency Inversion)**: Controllers depend on abstractions, not concrete implementations

## 📁 Project Structure (per service)

```
service/
├── src/
│   ├── index.ts                    # Entry point & bootstrap
│   ├── config/                     # Environment configuration
│   ├── domain/                     # Business logic (pure)
│   │   ├── model/                  # Domain entities
│   │   └── service/                # Service interfaces (contracts)
│   ├── application/                # Use cases / service implementations
│   └── infrastructure/             # External concerns
│       ├── persistence/            # Database access
│       ├── messaging/              # Kafka producers/consumers
│       └── web/                    # HTTP controllers & DTOs
└── tests/                          # Unit tests
```

## 🔑 Design Patterns Used

| Pattern | Where | Purpose |
|---------|-------|---------|
| Service Layer | application/ | Business logic orchestration |
| Repository | persistence/ | Data access abstraction |
| Observer/Pub-Sub | messaging/ | Async event communication |
| Saga | Order flow | Distributed transaction management |
| Factory | events.ts | Event creation helpers |
| DTO | web/dto.ts | API model separation from domain |

## ⚠️ Important Notes

- Services start with retry logic for Kafka/DB connections (infrastructure may take time to start)
- The Saga pattern provides eventual consistency, not immediate consistency
- Each service can be deployed, scaled, and updated independently
- Kafka ensures message delivery even if a service is temporarily down
