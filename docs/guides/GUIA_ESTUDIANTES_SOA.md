# Guía para Estudiantes: Arquitectura SOA

## Introducción

En este módulo implementamos una arquitectura **SOA (Service-Oriented Architecture)** con 3 servicios independientes que se comunican a través de Apache Kafka como Enterprise Service Bus (ESB).

SOA es un estilo arquitectónico donde la funcionalidad se organiza en servicios autónomos que se comunican mediante mensajes a través de un bus centralizado.

---

## Diferencias: SOA vs MVC vs Microservicios

| Aspecto | MVC | SOA | Microservicios |
|---|---|---|---|
| Despliegue | Monolito | Servicios independientes | Servicios independientes |
| Comunicación | Llamadas internas | ESB (Kafka) | REST + Eventos |
| Base de datos | Una compartida | Una por servicio | Una por servicio |
| Complejidad | Baja | Media | Alta |
| Servicios | 1 aplicación | 3 servicios | 8+ servicios |
| Resiliencia | N/A | Básica (retry) | Circuit Breaker, Saga, etc. |

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│              Enterprise Service Bus (Apache Kafka)            │
│                                                               │
│  Topics: product.created, order.created, order.confirmed,    │
│          order.cancelled, stock.reserved, stock.insufficient │
└──────┬──────────────────────┬──────────────────────┬────────┘
       │                      │                      │
 ┌─────▼─────┐         ┌─────▼─────┐         ┌─────▼─────┐
 │  Product   │         │   Order    │         │ Inventory  │
 │  Service   │         │  Service   │         │  Service   │
 │            │         │  (Saga)    │         │            │
 └─────┬─────┘         └─────┬─────┘         └─────┬─────┘
       │                      │                      │
 ┌─────▼─────┐         ┌─────▼─────┐         ┌─────▼─────┐
 │product_db  │         │ order_db   │         │inventory_db│
 └───────────┘         └───────────┘         └───────────┘
```

---

## Puertos por Lenguaje

| Lenguaje | Product | Order | Inventory | Kafka | PostgreSQL |
|---|---|---|---|---|---|
| **Java** | 8091 | 8092 | 8093 | 9092 | 5436 |
| **Node.js** | 4091 | 4092 | 4093 | 9093 | 5437 |
| **Python** | 9091 | 9092 | 9093 | 9094 | 5438 |
| **.NET** | 6091 | 6092 | 6093 | 9095 | 5439 |

---

## Levantar los Servicios

```bash
# Java
cd java-soa && docker compose up --build

# Node.js
cd nodejs-soa && docker compose up --build

# Python
cd python-soa && docker compose up --build

# .NET
cd dotnet-soa && docker compose up --build
```

---

## Saga Pattern: Flujo de Creación de Orden

### Happy Path
```
1. POST /api/orders → Order Service crea orden (PENDING)
2. Order Service publica "order.created" → Kafka
3. Inventory Service consume → reserva stock → publica "stock.reserved"
4. Order Service consume → confirma orden (CONFIRMED)
```

### Compensation (stock insuficiente)
```
1. POST /api/orders → Order Service crea orden (PENDING)
2. Order Service publica "order.created" → Kafka
3. Inventory Service consume → stock insuficiente → publica "stock.insufficient"
4. Order Service consume → cancela orden (CANCELLED)
```

---

## Ejemplo de Uso

```bash
# 1. Crear producto (ajusta el puerto según el lenguaje)
curl -X POST http://localhost:8091/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","description":"Gaming","price":999.99,"category":"Electronics","sku":"LAP-001"}'

# 2. Agregar inventario
curl -X POST http://localhost:8093/api/v1/inventory \
  -H "Content-Type: application/json" \
  -d '{"productId":"<ID>","productName":"Laptop","quantityAvailable":50}'

# 3. Crear orden (dispara saga)
curl -X POST http://localhost:8092/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-1","items":[{"productId":"<ID>","productName":"Laptop","quantity":2,"unitPrice":999.99}]}'

# 4. Verificar orden (debería ser CONFIRMED)
curl http://localhost:8092/api/v1/orders/<ORDER_ID>
```

---

## Detener

```bash
docker compose down      # Mantiene datos
docker compose down -v   # Borra datos
```
