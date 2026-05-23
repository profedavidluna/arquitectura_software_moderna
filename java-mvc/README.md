# Java MVC - Arquitecturas de Software

Este módulo implementa el mismo dominio de ecommerce (Product Catalog) usando tres patrones arquitectónicos diferentes en Java con Spring Boot.

## Arquitecturas Implementadas

### 1. Arquitectura Hexagonal (Ports & Adapters)
**Puerto**: 8081 | **Directorio**: `hexagonal-architecture/`

```
┌─────────────────────────────────────────────────┐
│                  ADAPTERS (Input)                │
│  ┌─────────────────────────────────────────┐    │
│  │  ProductController (REST)               │    │
│  └──────────────────┬──────────────────────┘    │
│                     │                           │
│  ┌──────────────────▼──────────────────────┐    │
│  │         INPUT PORT (Interface)          │    │
│  │         ProductServicePort              │    │
│  └──────────────────┬──────────────────────┘    │
│                     │                           │
│  ┌──────────────────▼──────────────────────┐    │
│  │         DOMAIN SERVICE                  │    │
│  │         ProductService                  │    │
│  │         (NO framework annotations)      │    │
│  └──────────────────┬──────────────────────┘    │
│                     │                           │
│  ┌──────────────────▼──────────────────────┐    │
│  │         OUTPUT PORT (Interface)         │    │
│  │         ProductRepositoryPort           │    │
│  └──────────────────┬──────────────────────┘    │
│                     │                           │
│  ┌──────────────────▼──────────────────────┐    │
│  │         ADAPTERS (Output)               │    │
│  │  ProductPersistenceAdapter (JPA)        │    │
│  └─────────────────────────────────────────┘    │
└─────────────────────────────────────────────────┘
```

**Características clave:**
- El dominio NO tiene dependencias de framework
- Los puertos son interfaces que definen contratos
- Los adaptadores implementan los puertos
- La configuración de Spring se hace en `BeanConfiguration`

### 2. Arquitectura Vertical Layer (Layered)
**Puerto**: 8082 | **Directorio**: `layered-architecture/`

```
┌─────────────────────────────────────────────────┐
│  PRESENTATION LAYER                             │
│  ┌─────────────────────────────────────────┐    │
│  │  ProductController + DTOs               │    │
│  └──────────────────┬──────────────────────┘    │
│                     │ depends on                │
│  ┌──────────────────▼──────────────────────┐    │
│  │  BUSINESS LAYER                         │    │
│  │  ProductService + Exceptions            │    │
│  └──────────────────┬──────────────────────┘    │
│                     │ depends on                │
│  ┌──────────────────▼──────────────────────┐    │
│  │  DATA LAYER                             │    │
│  │  ProductEntity + ProductRepository      │    │
│  └─────────────────────────────────────────┘    │
└─────────────────────────────────────────────────┘
```

**Características clave:**
- Dependencias fluyen hacia ABAJO (Presentation → Business → Data)
- Más simple y directo
- El servicio trabaja directamente con entidades JPA
- Trade-off: acoplamiento más fuerte entre capas

### 3. Arquitectura Limpia (Clean Architecture)
**Puerto**: 8083 | **Directorio**: `clean-architecture/`

```
┌─────────────────────────────────────────────────┐
│  FRAMEWORK LAYER (outermost)                    │
│  ┌─────────────────────────────────────────┐    │
│  │  REST Controller + JPA + Config         │    │
│  └──────────────────┬──────────────────────┘    │
│                     │                           │
│  ┌──────────────────▼──────────────────────┐    │
│  │  INTERFACE ADAPTERS LAYER               │    │
│  │  Gateway implementations + Presenters   │    │
│  └──────────────────┬──────────────────────┘    │
│                     │                           │
│  ┌──────────────────▼──────────────────────┐    │
│  │  USE CASES LAYER                        │    │
│  │  CreateProduct, GetProduct, etc.        │    │
│  │  (One class per use case)               │    │
│  └──────────────────┬──────────────────────┘    │
│                     │                           │
│  ┌──────────────────▼──────────────────────┐    │
│  │  ENTITIES LAYER (innermost)             │    │
│  │  Product (pure business rules)          │    │
│  └─────────────────────────────────────────┘    │
└─────────────────────────────────────────────────┘
```

**Características clave:**
- Dependency Rule: las capas internas NO conocen las externas
- Un Use Case por clase (Single Responsibility)
- Entities contienen reglas de negocio enterprise
- Use Cases contienen reglas de negocio de aplicación
- Framework es un detalle de implementación

## Cómo Ejecutar

```bash
# Hexagonal Architecture
cd hexagonal-architecture
mvn spring-boot:run

# Layered Architecture
cd layered-architecture
mvn spring-boot:run

# Clean Architecture
cd clean-architecture
mvn spring-boot:run
```

## API Endpoints (iguales en las 3 arquitecturas)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | /api/v1/products | Crear producto |
| GET | /api/v1/products | Listar productos |
| GET | /api/v1/products/{id} | Obtener producto |
| GET | /api/v1/products/search | Buscar productos |
| PUT | /api/v1/products/{id} | Actualizar producto |
| DELETE | /api/v1/products/{id} | Eliminar producto |
| PATCH | /api/v1/products/{id}/stock/decrease | Reducir stock |
| PATCH | /api/v1/products/{id}/stock/increase | Aumentar stock |

## Ejecutar Tests

```bash
cd hexagonal-architecture && mvn test
cd layered-architecture && mvn test
cd clean-architecture && mvn test
```

## Comparación de Arquitecturas

| Aspecto | Hexagonal | Layered | Clean |
|---------|-----------|---------|-------|
| Complejidad | Media | Baja | Alta |
| Testabilidad | Alta | Media | Muy Alta |
| Acoplamiento | Bajo | Alto | Muy Bajo |
| Curva de aprendizaje | Media | Baja | Alta |
| Flexibilidad | Alta | Baja | Muy Alta |
| Cantidad de código | Media | Baja | Alta |
| Independencia de framework | Sí | No | Sí |

## Tecnologías

- Java 17
- Spring Boot 3.2.5
- Spring Data JPA
- H2 Database (desarrollo)
- PostgreSQL (producción)
- JUnit 5 + Mockito (testing)
