# E-Commerce Microservices - .NET 8 / ASP.NET Core

## Descripción

Plataforma de e-commerce implementada con 8 microservicios independientes usando .NET 8, ASP.NET Core, Apache Kafka, PostgreSQL, Redis y Keycloak.

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                         Clientes                                 │
└──────────┬──────────────────────┬──────────────────────┬────────┘
           │                      │                      │
    ┌──────▼──────┐       ┌──────▼──────┐       ┌──────▼──────┐
    │UserService  │       │ProductService│       │ CartService │
    │  (6082)     │       │   (6083)     │       │   (6084)    │
    └─────────────┘       └─────────────┘       └─────────────┘
    ┌─────────────┐       ┌─────────────┐       ┌─────────────┐
    │OrderService │       │PaymentService│       │InventoryService│
    │  (6085)     │       │   (6086)     │       │   (6087)    │
    └─────────────┘       └─────────────┘       └─────────────┘
    ┌─────────────┐       ┌─────────────┐
    │Notification │       │ Analytics   │
    │  (6088)     │       │   (6089)    │
    └─────────────┘       └─────────────┘
           │                      │
    ┌──────▼──────────────────────▼──────────────────────────────┐
    │  Kafka (ESB) │ PostgreSQL │ Redis │ Keycloak               │
    └────────────────────────────────────────────────────────────┘
```

## Servicios

| Servicio | Puerto | BD | Descripción |
|---|---|---|---|
| UserService | 6082 | user_db | Gestión de usuarios y direcciones |
| ProductService | 6083 | product_db | Catálogo, categorías, reviews |
| CartService | 6084 | cart_db | Carrito de compras |
| OrderService | 6085 | order_db | Órdenes + Saga orchestrator |
| PaymentService | 6086 | payment_db | Pagos, refunds, multi-gateway |
| InventoryService | 6087 | inventory_db | Stock, reservas, alertas |
| NotificationService | 6088 | — | Emails via Kafka events |
| AnalyticsService | 6089 | analytics_db | Métricas y reportes |

## Ejecutar con Docker

```bash
cd services/dotnet-microservices
docker compose up --build -d
```

## Ejecutar Localmente

```bash
cd UserService
dotnet run
```

## Tests

```bash
cd UserService && dotnet test
cd ProductService && dotnet test
```

## Stack Tecnológico

- .NET 8 / ASP.NET Core
- Entity Framework Core 8
- Confluent.Kafka
- Npgsql (PostgreSQL)
- StackExchange.Redis
- Polly (Resilience)
- xUnit + Moq (Testing)
- Docker + Docker Compose
