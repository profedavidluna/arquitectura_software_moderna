# Python MVC - Product Catalog API

## Descripción

Implementación del mismo API de Catálogo de Productos usando **Python + FastAPI** con 3 patrones arquitectónicos diferentes. Cada implementación expone exactamente los mismos endpoints REST pero organiza el código de manera distinta.

## Arquitecturas Implementadas

| Arquitectura | Puerto | Descripción |
|---|---|---|
| **Hexagonal** | 8081 | Ports & Adapters - El dominio define interfaces (ports) que son implementadas por adaptadores |
| **Layered** | 8082 | Capas tradicionales - Presentation → Business → Data |
| **Clean** | 8083 | Clean Architecture de Uncle Bob - Un caso de uso por clase |

## Stack Tecnológico

- **Python 3.11**
- **FastAPI** - Framework web async de alto rendimiento
- **asyncpg** - Driver PostgreSQL async nativo
- **Pydantic v2** - Validación de datos y serialización
- **PostgreSQL 15** - Base de datos relacional
- **Docker & Docker Compose** - Contenedorización

## Inicio Rápido

### Con Docker (recomendado)

```bash
# Levantar todos los servicios
docker-compose up --build

# Los servicios estarán disponibles en:
# - Hexagonal: http://localhost:8081/api/v1/products
# - Layered:   http://localhost:8082/api/v1/products
# - Clean:     http://localhost:8083/api/v1/products
```

### Sin Docker (desarrollo local)

```bash
# Instalar dependencias (ejemplo con hexagonal)
cd hexagonal-architecture
pip install -r requirements.txt

# Ejecutar sin PostgreSQL (usa repositorio en memoria)
USE_POSTGRES=false uvicorn app.main:app --host 0.0.0.0 --port 8081 --reload

# Ejecutar con PostgreSQL
USE_POSTGRES=true DB_HOST=localhost DB_PORT=5433 DB_USER=postgres DB_PASSWORD=postgres DB_NAME=hexagonal_db \
  uvicorn app.main:app --host 0.0.0.0 --port 8081 --reload
```

### Ejecutar Tests

```bash
cd hexagonal-architecture
pip install -r requirements.txt
pytest tests/ -v
```

## API Endpoints

Todos los endpoints son idénticos en las 3 implementaciones:

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/v1/products` | Crear producto |
| GET | `/api/v1/products` | Listar productos (paginado) |
| GET | `/api/v1/products/{id}` | Obtener por ID |
| GET | `/api/v1/products/search` | Buscar productos |
| PUT | `/api/v1/products/{id}` | Actualizar producto |
| DELETE | `/api/v1/products/{id}` | Eliminar (soft delete) |
| PATCH | `/api/v1/products/{id}/stock/decrease?quantity=N` | Disminuir stock |
| PATCH | `/api/v1/products/{id}/stock/increase?quantity=N` | Aumentar stock |

## Ejemplos de Uso

```bash
# Crear producto
curl -X POST http://localhost:8081/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Gaming",
    "description": "Laptop para gaming de alta gama",
    "price": 1299.99,
    "category": "Electronics",
    "stock_quantity": 50,
    "sku": "LAP-GAME-001"
  }'

# Listar productos (paginado)
curl http://localhost:8081/api/v1/products?page=1&size=10

# Buscar productos
curl "http://localhost:8081/api/v1/products/search?query=laptop&category=Electronics"

# Disminuir stock
curl -X PATCH "http://localhost:8081/api/v1/products/{id}/stock/decrease?quantity=5"
```

## Reglas de Negocio

- El precio debe ser mayor a 0
- El stock no puede ser negativo
- El SKU debe ser único
- Disminuir stock falla si no hay suficiente
- Eliminar es soft-delete (active=false)
- Tamaño máximo de página: 100

## Comparación de Arquitecturas

### Hexagonal (Ports & Adapters)
- **Ventaja**: Dominio completamente aislado de infraestructura
- **Ventaja**: Fácil de testear con mocks
- **Desventaja**: Más archivos y abstracciones

### Layered (Capas)
- **Ventaja**: Simple y familiar
- **Ventaja**: Menos código boilerplate
- **Desventaja**: Acoplamiento entre capas

### Clean Architecture
- **Ventaja**: Un caso de uso = una clase (SRP)
- **Ventaja**: Regla de dependencia estricta
- **Desventaja**: Más clases para operaciones simples

## Estructura de Directorios

```
python-mvc/
├── docker-compose.yml
├── database/init.sql
├── hexagonal-architecture/   # Puerto 8081
├── layered-architecture/     # Puerto 8082
└── clean-architecture/       # Puerto 8083
```
