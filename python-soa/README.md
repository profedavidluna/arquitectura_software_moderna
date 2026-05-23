# SOA Architecture - Python + FastAPI + Kafka

## Arquitectura Orientada a Servicios (SOA)

Este proyecto implementa una arquitectura SOA completa con 3 servicios independientes que se comunican de forma asíncrona a través de Apache Kafka como Enterprise Service Bus (ESB).

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Enterprise Service Bus (Kafka)                     │
│                                                                           │
│  Topics: product.created | order.created | order.confirmed |              │
│          order.cancelled | stock.reserved | stock.insufficient |          │
│          stock.released                                                   │
└─────────┬──────────────────────┬──────────────────────┬─────────────────┘
          │                      │                      │
    ┌─────▼─────┐         ┌─────▼─────┐         ┌─────▼─────┐
    │  Product   │         │   Order    │         │ Inventory  │
    │  Service   │         │  Service   │         │  Service   │
    │  :9091     │         │  :9092     │         │  :9093     │
    └─────┬─────┘         └─────┬─────┘         └─────┬─────┘
          │                      │                      │
    ┌─────▼─────┐         ┌─────▼─────┐         ┌─────▼─────┐
    │product_db  │         │ order_db   │         │inventory_db│
    └───────────┘         └───────────┘         └───────────┘
```

## Servicios

| Servicio | Puerto | Responsabilidad |
|----------|--------|-----------------|
| Product Service | 9091 | Gestión del catálogo de productos |
| Order Service | 9092 | Gestión de pedidos + Orquestación Saga |
| Inventory Service | 9093 | Gestión de inventario/stock |

## Saga Pattern - Flujo de Creación de Pedido

```
┌──────────┐     ┌──────────────┐     ┌─────────────────┐     ┌──────────────┐
│  Cliente  │     │Order Service │     │      Kafka       │     │Inventory Svc │
└─────┬────┘     └──────┬───────┘     └────────┬────────┘     └──────┬───────┘
      │                  │                      │                      │
      │ POST /orders     │                      │                      │
      │─────────────────>│                      │                      │
      │                  │                      │                      │
      │  201 (PENDING)   │  order.created       │                      │
      │<─────────────────│─────────────────────>│                      │
      │                  │                      │  order.created        │
      │                  │                      │─────────────────────>│
      │                  │                      │                      │
      │                  │                      │    [Reserve Stock]    │
      │                  │                      │                      │
      │                  │                      │  stock.reserved       │
      │                  │  stock.reserved      │<─────────────────────│
      │                  │<─────────────────────│                      │
      │                  │                      │                      │
      │                  │  [Confirm Order]      │                      │
      │                  │  PENDING → CONFIRMED  │                      │
      │                  │                      │                      │
```

### Flujo de Compensación (Stock Insuficiente):

```
      │                  │                      │  stock.insufficient   │
      │                  │  stock.insufficient  │<─────────────────────│
      │                  │<─────────────────────│                      │
      │                  │                      │                      │
      │                  │  [Cancel Order]       │                      │
      │                  │  PENDING → CANCELLED  │                      │
```

## Principios SOA Implementados

1. **Service Autonomy**: Cada servicio tiene su propia base de datos y puede desplegarse independientemente
2. **Service Loose Coupling**: Comunicación asíncrona via eventos Kafka
3. **Service Abstraction**: Interfaces Protocol ocultan detalles de implementación
4. **Service Reusability**: Lógica de negocio encapsulada y reutilizable
5. **Service Composability**: El Order Service compone funcionalidad de múltiples servicios
6. **Service Statelessness**: Estado persistido en BD, servicios sin estado en memoria
7. **Service Discoverability**: APIs REST documentadas con OpenAPI/Swagger
8. **Standardized Service Contract**: Eventos con esquema consistente

## Principios SOLID

- **SRP**: Cada clase tiene una única responsabilidad
- **OCP**: Nuevos handlers de eventos sin modificar existentes
- **LSP**: Implementaciones sustituibles por interfaces Protocol
- **ISP**: Interfaces Protocol focalizadas y cohesivas
- **DIP**: Capas de aplicación dependen de abstracciones, no de concreciones

## Patrones de Diseño

- **Service Layer**: Protocol + Implementation
- **Repository**: Abstracción de acceso a datos
- **Observer/Pub-Sub**: Eventos Kafka
- **Saga (Orchestration)**: Transacción distribuida para creación de pedidos
- **Factory**: Creación de eventos
- **DTO**: Modelos Pydantic para API

## Stack Tecnológico

- Python 3.11
- FastAPI (framework web async)
- aiokafka (cliente Kafka asíncrono)
- asyncpg (cliente PostgreSQL asíncrono)
- Pydantic v2 (validación y serialización)
- Docker + Docker Compose

## Inicio Rápido

### Prerrequisitos
- Docker y Docker Compose instalados

### Ejecutar

```bash
# Clonar y navegar al directorio
cd python-soa

# Levantar toda la infraestructura y servicios
docker-compose up --build

# Los servicios estarán disponibles en:
# Product Service: http://localhost:9091
# Order Service:   http://localhost:9092
# Inventory Service: http://localhost:9093
```

### Documentación API (Swagger)

- Product Service: http://localhost:9091/docs
- Order Service: http://localhost:9092/docs
- Inventory Service: http://localhost:9093/docs

## Ejemplo de Uso (Flujo Completo)

```bash
# 1. Crear un producto
curl -X POST http://localhost:9091/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Gaming",
    "description": "High-performance gaming laptop",
    "price": 1299.99,
    "category": "Electronics",
    "sku": "LAP-GAME-001"
  }'

# 2. Crear inventario para el producto (usar el product_id del paso anterior)
curl -X POST http://localhost:9093/api/inventory \
  -H "Content-Type: application/json" \
  -d '{
    "product_id": "<PRODUCT_ID>",
    "product_name": "Laptop Gaming",
    "initial_quantity": 50
  }'

# 3. Crear un pedido (inicia la saga)
curl -X POST http://localhost:9092/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "items": [
      {
        "product_id": "<PRODUCT_ID>",
        "product_name": "Laptop Gaming",
        "quantity": 2,
        "unit_price": 1299.99
      }
    ]
  }'

# 4. Verificar el estado del pedido (debería ser CONFIRMED)
curl http://localhost:9092/api/orders/<ORDER_ID>

# 5. Verificar el inventario (stock reservado)
curl http://localhost:9093/api/inventory/<PRODUCT_ID>

# 6. Cancelar el pedido (trigger compensación)
curl -X POST http://localhost:9092/api/orders/<ORDER_ID>/cancel \
  -H "Content-Type: application/json" \
  -d '{"reason": "Customer changed mind"}'

# 7. Verificar inventario (stock liberado)
curl http://localhost:9093/api/inventory/<PRODUCT_ID>
```

## Ejecutar Tests

```bash
# Desde cada directorio de servicio
cd product-service
pip install -r requirements.txt
pytest app/tests/ -v

cd ../order-service
pip install -r requirements.txt
pytest app/tests/ -v

cd ../inventory-service
pip install -r requirements.txt
pytest app/tests/ -v
```

## Estructura del Proyecto

```
python-soa/
├── docker-compose.yml          # Orquestación de contenedores
├── database/init.sql           # Esquemas de BD
├── README.md                   # Este archivo
├── product-service/            # Servicio de Productos
│   ├── app/
│   │   ├── domain/             # Entidades y contratos
│   │   ├── application/        # Implementación de servicios
│   │   ├── infrastructure/     # Persistencia, mensajería, web
│   │   └── tests/              # Tests unitarios
│   ├── Dockerfile
│   └── requirements.txt
├── order-service/              # Servicio de Pedidos (Saga Orchestrator)
│   └── ... (misma estructura)
└── inventory-service/          # Servicio de Inventario (Saga Participant)
    └── ... (misma estructura)
```

## Temas Kafka

| Topic | Productor | Consumidor | Propósito |
|-------|-----------|------------|-----------|
| product.created | Product Service | - | Notifica nuevo producto |
| order.created | Order Service | Inventory Service | Inicia reserva de stock |
| stock.reserved | Inventory Service | Order Service | Confirma reserva exitosa |
| stock.insufficient | Inventory Service | Order Service | Indica stock insuficiente |
| order.confirmed | Order Service | - | Pedido confirmado |
| order.cancelled | Order Service | Inventory Service | Trigger liberación stock |
| stock.released | Inventory Service | - | Stock liberado |

## Health Checks

```bash
curl http://localhost:9091/health  # {"status": "UP", "service": "product-service"}
curl http://localhost:9092/health  # {"status": "UP", "service": "order-service"}
curl http://localhost:9093/health  # {"status": "UP", "service": "inventory-service"}
```
