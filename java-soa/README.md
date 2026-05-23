# Java SOA - Arquitectura Orientada a Servicios

## Descripción

Implementación de una arquitectura SOA (Service-Oriented Architecture) usando Java + Spring Boot con Apache Kafka como Enterprise Service Bus (ESB). El sistema implementa un dominio de ecommerce con 3 servicios independientes que se comunican de forma asíncrona.

## ¿Qué es SOA?

SOA es un estilo arquitectónico donde la funcionalidad se organiza en **servicios independientes** que se comunican a través de un **bus de servicios empresarial (ESB)**. Cada servicio:

- Tiene una responsabilidad bien definida
- Es independientemente desplegable
- Se comunica mediante mensajes/eventos
- Tiene su propia base de datos

### SOA vs Microservicios

| Aspecto | SOA | Microservicios |
|---|---|---|
| Comunicación | ESB centralizado (Kafka) | Punto a punto (REST/gRPC) |
| Granularidad | Servicios más grandes | Servicios más pequeños |
| Gobernanza | Centralizada | Descentralizada |
| Datos | BD compartida posible | BD por servicio obligatorio |
| Orquestación | Saga/Choreography | Choreography preferido |

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                    ENTERPRISE SERVICE BUS (Kafka)                 │
│                                                                   │
│  Topics: product.created, order.created, order.confirmed,        │
│          order.cancelled, stock.reserved, stock.insufficient,     │
│          stock.released                                           │
└──────┬──────────────────────┬──────────────────────┬────────────┘
       │                      │                      │
       ▼                      ▼                      ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────────┐
│   Product    │    │    Order     │    │    Inventory      │
│   Service    │    │   Service    │    │    Service        │
│   (8091)     │    │   (8092)     │    │    (8093)         │
├──────────────┤    ├──────────────┤    ├──────────────────┤
│ - CRUD       │    │ - Crear orden│    │ - Gestión stock   │
│ - Catálogo   │    │ - Estado     │    │ - Reservas        │
│ - Publicar   │    │ - Saga       │    │ - Compensación    │
│   eventos    │    │   orchestr.  │    │                   │
├──────────────┤    ├──────────────┤    ├──────────────────┤
│  product_db  │    │   order_db   │    │  inventory_db     │
└──────────────┘    └──────────────┘    └──────────────────┘
```

## Flujo de Eventos (Saga Pattern)

### Crear Orden (Happy Path)

```
1. Usuario → POST /api/v1/orders → Order Service
2. Order Service → publica "order.created" → Kafka
3. Inventory Service ← consume "order.created"
4. Inventory Service → reserva stock → publica "stock.reserved" → Kafka
5. Order Service ← consume "stock.reserved"
6. Order Service → confirma orden → publica "order.confirmed"
```

### Crear Orden (Stock Insuficiente - Compensación)

```
1. Usuario → POST /api/v1/orders → Order Service
2. Order Service → publica "order.created" → Kafka
3. Inventory Service ← consume "order.created"
4. Inventory Service → stock insuficiente → publica "stock.insufficient" → Kafka
5. Order Service ← consume "stock.insufficient"
6. Order Service → cancela orden → publica "order.cancelled"
```

## Patrones de Diseño Implementados

| Patrón | Dónde | Propósito |
|---|---|---|
| **Service Layer** | `domain/service/` | Interfaz de servicio bien definida |
| **Repository** | `infrastructure/persistence/` | Abstracción de acceso a datos |
| **Adapter** | `infrastructure/` | Conecta dominio con infraestructura |
| **Observer (Pub/Sub)** | Kafka producers/consumers | Comunicación desacoplada |
| **Saga** | Order + Inventory | Transacciones distribuidas |
| **Factory** | `Event.create()` | Creación de eventos |
| **DTO** | `web/dto/` | Separación API vs dominio |

## Principios SOLID

- **SRP**: Cada servicio tiene una sola responsabilidad
- **OCP**: Nuevos eventos se agregan sin modificar consumidores existentes
- **LSP**: Implementaciones son sustituibles por sus interfaces
- **ISP**: Interfaces focalizadas (ProductService, OrderService, InventoryService)
- **DIP**: El dominio depende de abstracciones, no de implementaciones concretas

## Cómo Ejecutar

### Con Docker (recomendado)

```bash
cd java-soa
docker compose up --build
```

Servicios disponibles:
- Product Service: http://localhost:8091
- Order Service: http://localhost:8092
- Inventory Service: http://localhost:8093
- Kafka: localhost:9092
- PostgreSQL: localhost:5436

### Sin Docker (desarrollo local)

Necesitas Kafka corriendo localmente en puerto 9092.

```bash
# Terminal 1 - Product Service
cd product-service && mvn spring-boot:run

# Terminal 2 - Order Service
cd order-service && mvn spring-boot:run

# Terminal 3 - Inventory Service
cd inventory-service && mvn spring-boot:run
```

## API Endpoints

### Product Service (8091)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | /api/v1/products | Crear producto |
| GET | /api/v1/products | Listar productos |
| GET | /api/v1/products/{id} | Obtener producto |
| GET | /api/v1/products/search | Buscar productos |
| PUT | /api/v1/products/{id} | Actualizar producto |
| DELETE | /api/v1/products/{id} | Eliminar producto |

### Order Service (8092)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | /api/v1/orders | Crear orden |
| GET | /api/v1/orders | Listar órdenes |
| GET | /api/v1/orders/{id} | Obtener orden |
| PATCH | /api/v1/orders/{id}/cancel | Cancelar orden |

### Inventory Service (8093)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | /api/v1/inventory | Agregar inventario |
| GET | /api/v1/inventory | Listar inventario |
| GET | /api/v1/inventory/{productId} | Obtener por producto |
| PATCH | /api/v1/inventory/{productId}/increase | Aumentar stock |

## Ejemplo de Uso Completo

```bash
# 1. Crear un producto
curl -X POST http://localhost:8091/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","description":"Gaming laptop","price":999.99,"category":"Electronics","sku":"LAP-001"}'

# 2. Agregar inventario para el producto
curl -X POST http://localhost:8093/api/v1/inventory \
  -H "Content-Type: application/json" \
  -d '{"productId":"<product-id>","productName":"Laptop","quantityAvailable":50}'

# 3. Crear una orden (dispara el saga)
curl -X POST http://localhost:8092/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-123","items":[{"productId":"<product-id>","productName":"Laptop","quantity":2,"unitPrice":999.99}]}'

# 4. Verificar estado de la orden (debería estar CONFIRMED)
curl http://localhost:8092/api/v1/orders/<order-id>

# 5. Verificar inventario (stock reservado)
curl http://localhost:8093/api/v1/inventory/<product-id>
```

## Tecnologías

- Java 17
- Spring Boot 3.2.5
- Spring Kafka
- Spring Data JPA
- PostgreSQL 15 (Docker) / H2 (local)
- Apache Kafka + Zookeeper
- JUnit 5 + Mockito
- Docker & Docker Compose

## Tests

```bash
cd product-service && mvn test
cd order-service && mvn test
cd inventory-service && mvn test
```
