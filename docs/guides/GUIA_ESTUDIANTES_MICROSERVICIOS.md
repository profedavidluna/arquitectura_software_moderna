# Guía para Estudiantes: Arquitectura de Microservicios

## Introducción

En este módulo implementamos una plataforma de e-commerce completa usando **Arquitectura de Microservicios**. El sistema está compuesto por 8 servicios independientes que se comunican de forma síncrona (REST) y asíncrona (Apache Kafka).

Cada servicio:
- Tiene su propia base de datos (Database per Service)
- Es independientemente desplegable
- Se comunica con otros servicios mediante eventos
- Implementa patrones de resiliencia (Circuit Breaker, Retry, Timeout)

La misma arquitectura está implementada en **4 lenguajes**: Java, .NET, Node.js y Python.

---

## Prerrequisitos

| Herramienta | Versión mínima | Verificar con |
|---|---|---|
| Docker | 20.10+ | `docker --version` |
| Docker Compose | 2.0+ | `docker compose version` |
| Git | 2.30+ | `git --version` |
| **RAM disponible** | **8 GB mínimo** (16 GB recomendado) | — |

> **Importante**: Los microservicios requieren más recursos que MVC o SOA porque levantan más contenedores simultáneamente (8 servicios + Kafka + PostgreSQL + Redis + Keycloak).

---

## Los 8 Microservicios

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              CLIENTES                                    │
└────────┬────────────────┬────────────────┬────────────────┬─────────────┘
         │                │                │                │
   ┌─────▼─────┐   ┌─────▼─────┐   ┌─────▼─────┐   ┌─────▼─────┐
   │   User    │   │  Product  │   │   Cart    │   │   Order   │
   │  Service  │   │  Service  │   │  Service  │   │  Service  │
   │           │   │           │   │           │   │  (Saga)   │
   └─────┬─────┘   └─────┬─────┘   └─────┬─────┘   └─────┬─────┘
         │                │                │                │
   ┌─────▼─────┐   ┌─────▼─────┐   ┌─────▼─────┐   ┌─────▼─────┐
   │  Payment  │   │ Inventory │   │Notification│   │ Analytics │
   │  Service  │   │  Service  │   │  Service   │   │  Service  │
   └───────────┘   └───────────┘   └───────────┘   └───────────┘
         │                │                │                │
         └────────────────┴────────────────┴────────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
        ┌─────▼─────┐      ┌─────▼─────┐      ┌─────▼─────┐
        │   Kafka   │      │ PostgreSQL│      │   Redis   │
        │   (ESB)   │      │  (7 BDs)  │      │  (Cache)  │
        └───────────┘      └───────────┘      └───────────┘
```

### Responsabilidades de cada servicio

| Servicio | Responsabilidad | Patrones clave |
|---|---|---|
| **User Service** | Registro, autenticación, perfiles, direcciones | CRUD, eventos de usuario |
| **Product Service** | Catálogo, categorías, reviews, búsqueda | Paginación, cache, full-text search |
| **Cart Service** | Carrito de compras, cálculos, cupones | Redis cache, TTL, validación de stock |
| **Order Service** | Creación de órdenes, estado, historial | **Saga Orchestrator**, state machine |
| **Payment Service** | Procesamiento de pagos, refunds | Strategy (Stripe/PayPal), retry, idempotencia |
| **Inventory Service** | Stock, reservas, alertas de bajo stock | Optimistic locking, eventos |
| **Notification Service** | Emails (confirmación, recibo, envío) | Event consumer, templates |
| **Analytics Service** | Métricas, reportes, dashboards | CQRS, event sourcing, agregaciones |

---

## Comunicación entre Servicios

### Síncrona (REST con Circuit Breaker)

```
Cart Service ──HTTP──▶ Product Service    (validar precio/stock)
Order Service ──HTTP──▶ Cart Service      (obtener carrito para checkout)
Order Service ──HTTP──▶ Inventory Service (reservar stock)
Order Service ──HTTP──▶ Payment Service   (procesar pago)
```

Cada llamada REST incluye:
- **Circuit Breaker**: Si el servicio destino falla 5 veces, se abre el circuito por 30s
- **Retry**: 3 reintentos con backoff exponencial (1s, 2s, 4s)
- **Timeout**: Máximo 10 segundos por llamada

### Asíncrona (Apache Kafka)

```
┌──────────────┐                              ┌──────────────────┐
│ Order Service│──── order-events ───────────▶│Notification Svc  │
│              │──── order-events ───────────▶│Inventory Service │
│              │──── order-events ───────────▶│Analytics Service │
└──────────────┘                              └──────────────────┘

┌──────────────┐                              ┌──────────────────┐
│Payment Service│──── payment-events ────────▶│ Order Service    │
│              │──── payment-events ────────▶│Notification Svc  │
└──────────────┘                              └──────────────────┘

┌──────────────┐                              ┌──────────────────┐
│Inventory Svc │──── inventory-events ───────▶│ Order Service    │
│              │──── inventory-events ───────▶│Analytics Service │
└──────────────┘                              └──────────────────┘
```

---

## Saga Pattern: Creación de Orden

El flujo más complejo del sistema. El Order Service actúa como **Saga Orchestrator**:

### Happy Path (todo sale bien)

```
1. Cliente ──POST /orders──▶ Order Service
2. Order Service ──▶ Cart Service: obtener carrito ✓
3. Order Service ──▶ Inventory Service: reservar stock ✓
4. Order Service ──▶ Payment Service: procesar pago ✓
5. Order Service: confirmar orden (PENDING → CONFIRMED)
6. Order Service ──publish──▶ Kafka: "order.confirmed"
7. Notification Service: enviar email de confirmación
8. Analytics Service: registrar métricas
```

### Compensation Path (algo falla)

```
1. Cliente ──POST /orders──▶ Order Service
2. Order Service ──▶ Cart Service: obtener carrito ✓
3. Order Service ──▶ Inventory Service: reservar stock ✓
4. Order Service ──▶ Payment Service: procesar pago ✗ (FALLA)
5. COMPENSACIÓN:
   a. Order Service ──▶ Inventory Service: liberar stock (rollback)
   b. Order Service: cancelar orden (PENDING → CANCELLED)
6. Order Service ──publish──▶ Kafka: "order.cancelled"
```

---

## Puertos por Lenguaje

### Java (Spring Boot)

| Servicio | Puerto | Infraestructura | Puerto |
|---|---|---|---|
| User Service | 8082 | Kafka | 9092 |
| Product Service | 8083 | PostgreSQL | 5432 |
| Cart Service | 8084 | Redis | 6379 |
| Order Service | 8085 | Keycloak | 8180 |
| Payment Service | 8086 | | |
| Inventory Service | 8087 | | |
| Notification Service | 8088 | | |
| Analytics Service | 8089 | | |

### .NET (ASP.NET Core)

| Servicio | Puerto |
|---|---|
| User Service | 6082 |
| Product Service | 6083 |
| Cart Service | 6084 |
| Order Service | 6085 |
| Payment Service | 6086 |
| Inventory Service | 6087 |
| Notification Service | 6088 |
| Analytics Service | 6089 |

### Node.js (Express + TypeScript)

Consultar `services/nodejs-microservices/docker-compose.yml` para puertos específicos.

### Python (FastAPI)

Consultar `services/python-microservices/docker-compose.yml` para puertos específicos.

---

## Levantar los Microservicios

### Paso 1: Elegir el lenguaje

```bash
# Java
cd services/java-microservices

# .NET
cd services/dotnet-microservices

# Node.js
cd services/nodejs-microservices

# Python
cd services/python-microservices
```

### Paso 2: Levantar todo

```bash
# Construir y levantar (primera vez toma varios minutos)
docker compose up --build -d

# Ver logs en tiempo real
docker compose logs -f

# Ver estado de los servicios
docker compose ps
```

### Paso 3: Esperar a que todo esté listo

Los servicios tienen health checks. Espera hasta que todos muestren `healthy`:

```bash
# Verificar health de cada servicio
docker compose ps
```

Típicamente toma 1-3 minutos para que todo esté listo (Kafka y Keycloak son los más lentos).

### Paso 4: Verificar

```bash
# Java
curl http://localhost:8082/actuator/health  # User Service
curl http://localhost:8083/actuator/health  # Product Service

# .NET
curl http://localhost:6082/health
curl http://localhost:6083/health
```

### Paso 5: Detener

```bash
docker compose down       # Detener (mantiene datos)
docker compose down -v    # Detener y borrar datos
```

---

## Levantar Solo Infraestructura

Si quieres desarrollar un servicio localmente pero necesitas Kafka, PostgreSQL, etc.:

```bash
# Solo infraestructura
docker compose up -d zookeeper kafka postgres redis keycloak

# Luego ejecuta tu servicio localmente
cd user-service
# Java: mvn spring-boot:run
# .NET: dotnet run
# Node: npm run dev
# Python: uvicorn app.main:app --reload
```

---

## Probando el Flujo Completo

### 1. Crear un usuario

```bash
curl -X POST http://localhost:8082/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@example.com",
    "password": "SecurePass123!",
    "firstName": "Juan",
    "lastName": "García",
    "phone": "+57 300 123 4567"
  }'
```

### 2. Crear un producto

```bash
curl -X POST http://localhost:8083/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro M3",
    "description": "Laptop profesional con chip M3",
    "price": 2499.99,
    "category": "Electronics",
    "sku": "MBP-M3-001"
  }'
```

### 3. Agregar inventario

```bash
curl -X POST http://localhost:8087/api/v1/inventory \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "<PRODUCT_ID>",
    "sku": "MBP-M3-001",
    "quantityAvailable": 100,
    "reorderPoint": 10
  }'
```

### 4. Agregar al carrito

```bash
curl -X POST http://localhost:8084/api/v1/carts \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "<USER_ID>"
  }'

curl -X POST http://localhost:8084/api/v1/carts/<CART_ID>/items \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "<PRODUCT_ID>",
    "productName": "MacBook Pro M3",
    "sku": "MBP-M3-001",
    "quantity": 1,
    "unitPrice": 2499.99
  }'
```

### 5. Crear orden (dispara la Saga)

```bash
curl -X POST http://localhost:8085/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "<USER_ID>",
    "cartId": "<CART_ID>",
    "shippingAddress": {
      "street": "Calle 100 #15-20",
      "city": "Bogotá",
      "state": "Cundinamarca",
      "zipCode": "110111",
      "country": "Colombia"
    }
  }'
```

### 6. Verificar estado de la orden

```bash
# Debería estar en CONFIRMED si todo salió bien
curl http://localhost:8085/api/v1/orders/<ORDER_ID>
```

### 7. Verificar inventario (stock reservado)

```bash
curl http://localhost:8087/api/v1/inventory/<PRODUCT_ID>
# quantityReserved debería haber aumentado
```

---

## Patrones de Diseño Implementados

### 1. Database per Service
Cada servicio tiene su propia base de datos PostgreSQL. No comparten tablas.

```
User Service       → user_db
Product Service    → product_db
Cart Service       → cart_db
Order Service      → order_db
Payment Service    → payment_db
Inventory Service  → inventory_db
Analytics Service  → analytics_db
```

### 2. Saga Pattern (Orchestration)
El Order Service coordina la transacción distribuida de creación de orden.

### 3. Circuit Breaker
Previene cascadas de fallos cuando un servicio está caído.

```
Estado CLOSED → llamadas pasan normalmente
  ↓ (5 fallos consecutivos)
Estado OPEN → llamadas fallan inmediatamente (30s)
  ↓ (después de 30s)
Estado HALF-OPEN → permite 3 llamadas de prueba
  ↓ (si pasan)
Estado CLOSED → vuelve a la normalidad
```

### 4. Event-Driven Architecture
Los servicios publican eventos a Kafka cuando algo importante sucede.

### 5. CQRS (Command Query Responsibility Segregation)
Analytics Service consume eventos (escritura) y sirve reportes (lectura) con modelos optimizados.

### 6. Strategy Pattern
Payment Service usa diferentes estrategias para Stripe vs PayPal.

### 7. Repository Pattern
Cada servicio abstrae el acceso a datos detrás de una interfaz.

---

## Diferencias: SOA vs Microservicios

| Aspecto | SOA (módulo anterior) | Microservicios (este módulo) |
|---|---|---|
| Servicios | 3 (Product, Order, Inventory) | 8 servicios especializados |
| Comunicación | Solo Kafka (async) | REST + Kafka (sync + async) |
| Resiliencia | Básica | Circuit Breaker, Retry, Timeout, Bulkhead |
| Autenticación | No | Keycloak (OAuth2/OIDC) |
| Cache | No | Redis |
| Transacciones | Saga simple | Saga con compensación completa |
| Observabilidad | Logs básicos | Prometheus, Grafana, Jaeger |
| Complejidad | Media | Alta |
| Recursos | ~4 GB RAM | ~8-16 GB RAM |

---

## Monitoreo y Observabilidad

### Health Checks

Cada servicio expone un endpoint de salud:
```bash
curl http://localhost:8082/actuator/health  # Java
curl http://localhost:6082/health           # .NET
```

### Métricas (Prometheus)

```bash
curl http://localhost:8082/actuator/prometheus  # Java
```

### Logs Estructurados

Todos los servicios usan logging estructurado con correlation IDs:
```json
{
  "timestamp": "2025-05-20T10:30:45Z",
  "level": "INFO",
  "service": "order-service",
  "traceId": "abc123def456",
  "message": "Order created",
  "orderId": "550e8400-..."
}
```

---

## Solución de Problemas

### Los servicios no arrancan

**Causa común**: Kafka o PostgreSQL no están listos.

```bash
# Ver logs de infraestructura
docker compose logs kafka
docker compose logs postgres

# Reiniciar un servicio específico
docker compose restart order-service
```

### Error "Connection refused" entre servicios

**Causa**: El servicio destino aún no está listo.

**Solución**: Los Circuit Breakers manejan esto automáticamente. Espera 30-60 segundos.

### Kafka consumer lag (mensajes no se procesan)

```bash
# Ver consumer groups
docker compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:29092 --list

# Ver lag de un grupo
docker compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:29092 \
  --group order-service-group --describe
```

### Memoria insuficiente

Si Docker se queda sin memoria:
```bash
# Reducir servicios (levantar solo los necesarios)
docker compose up -d postgres kafka redis user-service product-service

# O aumentar memoria de Docker Desktop (Settings → Resources → Memory)
```

### Limpiar todo y empezar de cero

```bash
docker compose down -v --rmi local
docker system prune -f
docker compose up --build -d
```

---

## Ejercicios Sugeridos

### Nivel Básico
1. **Crear un producto y verificar** que aparece en el catálogo
2. **Agregar al carrito** y verificar que el total se calcula correctamente
3. **Crear una orden** y seguir el flujo en los logs de cada servicio

### Nivel Intermedio
4. **Simular fallo de inventario**: Intenta crear una orden con más cantidad que el stock disponible. Verifica que la saga hace rollback.
5. **Observar el Circuit Breaker**: Detén el Payment Service y crea una orden. ¿Qué pasa?
6. **Agregar un campo**: Agrega `weight` al producto. ¿Cuántos servicios necesitas modificar?

### Nivel Avanzado
7. **Implementar un nuevo servicio**: Crea un "Shipping Service" que consuma eventos `order.confirmed` y publique `order.shipped`.
8. **Agregar rate limiting**: Implementa rate limiting en el API Gateway (100 req/s por usuario).
9. **Implementar retry con dead letter queue**: Cuando un mensaje Kafka falla 3 veces, envíalo a un DLQ.

---

## Resumen de Comandos

| Acción | Comando |
|---|---|
| Levantar todo | `docker compose up --build -d` |
| Ver logs | `docker compose logs -f` |
| Logs de un servicio | `docker compose logs -f order-service` |
| Estado | `docker compose ps` |
| Detener | `docker compose down` |
| Detener + borrar datos | `docker compose down -v` |
| Reiniciar un servicio | `docker compose restart order-service` |
| Entrar a PostgreSQL | `docker compose exec postgres psql -U postgres` |
| Ver topics Kafka | `docker compose exec kafka kafka-topics --bootstrap-server localhost:29092 --list` |
| Ver mensajes de un topic | `docker compose exec kafka kafka-console-consumer --bootstrap-server localhost:29092 --topic order-events --from-beginning` |
| Entrar a Redis | `docker compose exec redis redis-cli` |

---

## Recursos Adicionales

- [Microservices Patterns (Chris Richardson)](https://microservices.io/patterns/)
- [Building Microservices (Sam Newman)](https://samnewman.io/books/building_microservices_2nd_edition/)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
