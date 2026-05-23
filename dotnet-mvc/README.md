# .NET MVC - Arquitecturas de Software

Este módulo implementa el mismo dominio de ecommerce (Product Catalog) usando tres patrones arquitectónicos diferentes en C# con ASP.NET Core.

## Arquitecturas Implementadas

### 1. Arquitectura Hexagonal (Ports & Adapters)
**Puerto**: 5081 | **Directorio**: `HexagonalArchitecture/`

```
┌─────────────────────────────────────────────────┐
│                  ADAPTERS (Input)                │
│  ┌─────────────────────────────────────────┐    │
│  │  ProductController (REST)               │    │
│  └──────────────────┬──────────────────────┘    │
│                     │                           │
│  ┌──────────────────▼──────────────────────┐    │
│  │         INPUT PORT (Interface)          │    │
│  │         IProductService                 │    │
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
│  │         IProductRepository              │    │
│  └──────────────────┬──────────────────────┘    │
│                     │                           │
│  ┌──────────────────▼──────────────────────┐    │
│  │         ADAPTERS (Output)               │    │
│  │  ProductRepositoryAdapter (EF Core)     │    │
│  └─────────────────────────────────────────┘    │
└─────────────────────────────────────────────────┘
```

**Características clave:**
- El dominio NO tiene dependencias de framework
- Los puertos son interfaces que definen contratos
- Los adaptadores implementan los puertos
- La configuración de DI se hace en `Program.cs` (sin atributos)

### 2. Arquitectura por Capas (Layered)
**Puerto**: 5082 | **Directorio**: `LayeredArchitecture/`

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
- El servicio trabaja directamente con entidades EF Core
- Trade-off: acoplamiento más fuerte entre capas

### 3. Arquitectura Limpia (Clean Architecture)
**Puerto**: 5083 | **Directorio**: `CleanArchitecture/`

```
┌─────────────────────────────────────────────────┐
│  FRAMEWORK LAYER (outermost)                    │
│  ┌─────────────────────────────────────────┐    │
│  │  REST Controller + EF Core + Config     │    │
│  └──────────────────┬──────────────────────┘    │
│                     │                           │
│  ┌──────────────────▼──────────────────────┐    │
│  │  INTERFACE ADAPTERS LAYER               │    │
│  │  Gateway implementations                │    │
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
cd HexagonalArchitecture
dotnet run

# Layered Architecture
cd LayeredArchitecture
dotnet run

# Clean Architecture
cd CleanArchitecture
dotnet run
```

## API Endpoints (iguales en las 3 arquitecturas)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | /api/v1/products | Crear producto |
| GET | /api/v1/products | Listar productos (paginado) |
| GET | /api/v1/products/{id} | Obtener producto por ID |
| GET | /api/v1/products/search | Buscar productos |
| PUT | /api/v1/products/{id} | Actualizar producto |
| DELETE | /api/v1/products/{id} | Eliminar producto (soft-delete) |
| PATCH | /api/v1/products/{id}/stock/decrease?quantity=N | Reducir stock |
| PATCH | /api/v1/products/{id}/stock/increase?quantity=N | Aumentar stock |

## Ejemplo de Uso (curl)

```bash
# Crear producto
curl -X POST http://localhost:5081/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Gaming",
    "description": "Laptop de alto rendimiento",
    "price": 1299.99,
    "category": "Electronics",
    "stockQuantity": 50,
    "sku": "LAP-GAM-001"
  }'

# Listar productos
curl http://localhost:5081/api/v1/products?page=0&pageSize=10

# Buscar productos
curl http://localhost:5081/api/v1/products/search?query=laptop

# Reducir stock
curl -X PATCH "http://localhost:5081/api/v1/products/{id}/stock/decrease?quantity=5"
```

## Ejecutar Tests

```bash
cd HexagonalArchitecture && dotnet test
cd LayeredArchitecture && dotnet test
cd CleanArchitecture && dotnet test
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

## Reglas de Negocio

- **SKU único**: No pueden existir dos productos con el mismo SKU
- **Precio positivo**: El precio debe ser mayor a 0
- **Stock no negativo**: El stock no puede ser menor a 0
- **Paginación**: Tamaño máximo de página es 100
- **Soft-delete**: Eliminar un producto solo lo desactiva (active=false)

## Tecnologías

- .NET 8
- ASP.NET Core Web API
- Entity Framework Core (InMemory provider para desarrollo)
- xUnit + Moq (testing)
- Swagger/OpenAPI (documentación)
- ProblemDetails (manejo de errores RFC 7807)
