# Inventory Service

E-Commerce Inventory Management Microservice built with Spring Boot 3.4.5 and Java 21.

## Overview

The Inventory Service manages stock levels, reservations, and inventory transactions for the e-commerce platform. It provides atomic stock operations with optimistic locking, low stock alerts, and event-driven integration with other services via Apache Kafka.

## Architecture

- **Framework**: Spring Boot 3.4.5
- **Language**: Java 21
- **Database**: PostgreSQL (port 5435, database: `inventory_db`)
- **Messaging**: Apache Kafka
- **Security**: OAuth2 Resource Server (Keycloak)
- **Observability**: Micrometer + Prometheus + OpenTelemetry

## Features

- **Stock Management**: Create, update, and query inventory records
- **Stock Reservation**: Atomic reserve with optimistic locking for concurrent order processing
- **Stock Depletion**: Convert reserved stock to depleted after shipment confirmation
- **Stock Release**: Return reserved stock to available when orders are cancelled
- **Restocking**: Add inventory with max quantity validation
- **Low Stock Alerts**: Auto-generated alerts when stock falls below reorder point
- **Inventory Reconciliation**: Compare expected vs actual quantities with adjustment transactions
- **Transaction History**: Complete audit trail of all inventory changes
- **Event-Driven**: Publishes and consumes Kafka events for cross-service coordination

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/inventory/{productId}` | Get stock for a product |
| GET | `/api/v1/inventory` | List all inventory (paginated) |
| POST | `/api/v1/inventory` | Create inventory record |
| PUT | `/api/v1/inventory/{productId}` | Update inventory configuration |
| POST | `/api/v1/inventory/{productId}/reserve` | Reserve stock |
| POST | `/api/v1/inventory/{productId}/release` | Release reserved stock |
| POST | `/api/v1/inventory/{productId}/deplete` | Deplete stock (after shipment) |
| POST | `/api/v1/inventory/{productId}/restock` | Restock inventory |
| GET | `/api/v1/inventory/low-stock` | Get low stock items |
| GET | `/api/v1/inventory/{productId}/transactions` | Get transaction history |
| GET | `/api/v1/inventory/alerts` | Get active low stock alerts |
| POST | `/api/v1/inventory/{productId}/reconcile` | Reconcile inventory |

## Kafka Events

### Published Events
- `inventory.reserved` - When stock is reserved for an order
- `inventory.depleted` - When reserved stock is depleted (shipped)

### Consumed Events
- `order.created` - Auto-reserves stock for order items
- `order.cancelled` - Auto-releases reserved stock

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL 15+ (port 5435)
- Apache Kafka
- Keycloak (for OAuth2)

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
docker build -t inventory-service .
docker run -p 8085:8085 inventory-service
```

## Configuration

The service uses Spring profiles for environment-specific configuration:

- **default**: Production-ready settings
- **dev**: Development with debug logging and auto DDL
- **test**: H2 in-memory database, Kafka disabled
- **prod**: Minimal logging, strict validation

## API Documentation

When running locally, access the Swagger UI at:
- http://localhost:8085/swagger-ui.html
- http://localhost:8085/v3/api-docs

## Health & Metrics

- Health: http://localhost:8085/actuator/health
- Metrics: http://localhost:8085/actuator/metrics
- Prometheus: http://localhost:8085/actuator/prometheus

## Database Schema

The service uses the `inventory_db` database with the following tables:
- `inventory` - Current stock levels per product
- `inventory_transactions` - Audit trail of all stock changes
- `low_stock_alerts` - Alerts for products below reorder point

See `shared/database-schemas/inventory-db-schema.sql` for the complete schema.

## Testing

- **Unit Tests**: Service and controller layer with Mockito
- **Integration Tests**: Full stack with TestContainers (PostgreSQL)
- **Contract Tests**: Pact consumer tests for API contracts
- **Coverage Target**: 80% (enforced by JaCoCo)

## Project Structure

```
src/main/java/com/ecommerce/inventoryservice/
├── InventoryServiceApplication.java
├── config/
│   ├── KafkaConfig.java
│   ├── OpenApiConfig.java
│   └── SecurityConfig.java
├── controller/
│   └── InventoryController.java
├── dto/
│   ├── CreateInventoryRequest.java
│   ├── UpdateInventoryRequest.java
│   ├── ReserveStockRequest.java
│   ├── ReleaseStockRequest.java
│   ├── DepleteStockRequest.java
│   ├── RestockRequest.java
│   ├── ReconciliationRequest.java
│   ├── InventoryResponse.java
│   ├── TransactionResponse.java
│   └── LowStockAlertResponse.java
├── entity/
│   ├── Inventory.java
│   ├── InventoryTransaction.java
│   ├── LowStockAlert.java
│   ├── TransactionType.java
│   ├── ReferenceType.java
│   └── AlertStatus.java
├── event/
│   ├── InventoryEvent.java
│   ├── InventoryEventPublisher.java
│   ├── OrderEvent.java
│   └── OrderEventConsumer.java
├── exception/
│   ├── DuplicateInventoryException.java
│   ├── GlobalExceptionHandler.java
│   ├── InsufficientStockException.java
│   └── InventoryNotFoundException.java
├── mapper/
│   └── InventoryMapper.java
├── repository/
│   ├── InventoryRepository.java
│   ├── InventoryTransactionRepository.java
│   └── LowStockAlertRepository.java
└── service/
    └── InventoryService.java
```
