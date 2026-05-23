# Guía para Estudiantes: Arquitecturas MVC con Docker

## Introducción

En este módulo del curso implementamos el mismo dominio de negocio (**Catálogo de Productos**) usando tres patrones arquitectónicos diferentes:

1. **Arquitectura Hexagonal** (Ports & Adapters)
2. **Arquitectura por Capas** (Layered / Vertical Layer)
3. **Arquitectura Limpia** (Clean Architecture)

Cada patrón está implementado en **4 lenguajes**: Java, .NET, Node.js y Python. Todas las implementaciones exponen exactamente la misma API REST, lo que permite comparar cómo cada arquitectura organiza el código de manera diferente para resolver el mismo problema.

---

## Prerrequisitos

Antes de comenzar, asegúrate de tener instalado:

| Herramienta | Versión mínima | Verificar con |
|---|---|---|
| Docker | 20.10+ | `docker --version` |
| Docker Compose | 2.0+ | `docker compose version` |
| Git | 2.30+ | `git --version` |

**Opcional** (solo si quieres ejecutar sin Docker):

| Herramienta | Versión | Para |
|---|---|---|
| Java JDK | 17+ | `java --version` |
| Maven | 3.8+ | `mvn --version` |
| .NET SDK | 8.0+ | `dotnet --version` |
| Node.js | 18+ | `node --version` |
| Python | 3.10+ | `python --version` |

---

## Estructura del Proyecto

```
arquitecturadesoftware/
├── java-mvc/                    # Implementaciones en Java (Spring Boot)
│   ├── hexagonal-architecture/
│   ├── layered-architecture/
│   ├── clean-architecture/
│   ├── database/init.sql
│   └── docker-compose.yml
│
├── dotnet-mvc/                  # Implementaciones en .NET (ASP.NET Core)
│   ├── HexagonalArchitecture/
│   ├── LayeredArchitecture/
│   ├── CleanArchitecture/
│   ├── database/init.sql
│   └── docker-compose.yml
│
├── nodejs-mvc/                  # Implementaciones en Node.js (Express + TypeScript)
│   ├── hexagonal-architecture/
│   ├── layered-architecture/
│   ├── clean-architecture/
│   ├── database/init.sql
│   └── docker-compose.yml
│
└── python-mvc/                  # Implementaciones en Python (FastAPI)
    ├── hexagonal-architecture/
    ├── layered-architecture/
    ├── clean-architecture/
    ├── database/init.sql
    └── docker-compose.yml
```

---

## Puertos Asignados

Cada lenguaje tiene su propio rango de puertos para evitar conflictos:

### Puertos de las Aplicaciones

| Lenguaje | Hexagonal | Layered | Clean |
|---|---|---|---|
| **Java** | 8081 | 8082 | 8083 |
| **.NET** | 5081 | 5082 | 5083 |
| **Node.js** | 3081 | 3082 | 3083 |
| **Python** | 8081 | 8082 | 8083 |

### Puertos de PostgreSQL

| Lenguaje | Puerto externo | Puerto interno |
|---|---|---|
| **Node.js** | 5432 | 5432 |
| **Python** | 5433 | 5432 |
| **Java** | 5434 | 5432 |
| **.NET** | 5435 | 5432 |

> **Nota**: Los puertos de Python y Java para las apps coinciden (8081-8083), así que no ejecutes ambos Docker Compose al mismo tiempo. Elige uno a la vez o modifica los puertos.

---

## Levantar los Proyectos con Docker

### Paso 1: Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd arquitecturadesoftware
```

### Paso 2: Elegir el lenguaje a ejecutar

#### Java (Spring Boot)

```bash
cd java-mvc
docker compose up --build
```

Esto levanta:
- PostgreSQL en puerto **5434** con 3 bases de datos
- Hexagonal Architecture en http://localhost:8081
- Layered Architecture en http://localhost:8082
- Clean Architecture en http://localhost:8083

#### .NET (ASP.NET Core)

```bash
cd dotnet-mvc
docker compose up --build
```

Esto levanta:
- PostgreSQL en puerto **5435** con 3 bases de datos
- Hexagonal Architecture en http://localhost:5081
- Layered Architecture en http://localhost:5082
- Clean Architecture en http://localhost:5083

#### Node.js (Express + TypeScript)

```bash
cd nodejs-mvc
docker compose up --build
```

Esto levanta:
- PostgreSQL en puerto **5432** con 3 bases de datos
- Hexagonal Architecture en http://localhost:3081
- Layered Architecture en http://localhost:3082
- Clean Architecture en http://localhost:3083

#### Python (FastAPI)

```bash
cd python-mvc
docker compose up --build
```

Esto levanta:
- PostgreSQL en puerto **5433** con 3 bases de datos
- Hexagonal Architecture en http://localhost:8081
- Layered Architecture en http://localhost:8082
- Clean Architecture en http://localhost:8083

### Paso 3: Verificar que todo funciona

Espera a que todos los servicios estén listos (verás logs indicando que cada app está corriendo). Luego prueba:

```bash
# Health check (ajusta el puerto según el lenguaje)
curl http://localhost:8081/health
```

### Paso 4: Detener los servicios

```bash
docker compose down
```

Para eliminar también los volúmenes de datos:

```bash
docker compose down -v
```

---

## Probando la API

Todas las implementaciones exponen los mismos endpoints. Aquí usamos el puerto 8081 (Java Hexagonal) como ejemplo. Ajusta el puerto según el lenguaje y arquitectura que estés probando.

### Crear un producto

```bash
curl -X POST http://localhost:8081/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Gaming Pro",
    "description": "Laptop de alta gama para gaming con RTX 4080",
    "price": 1599.99,
    "category": "Electronics",
    "stockQuantity": 25,
    "sku": "LAP-GAME-001"
  }'
```

**Respuesta esperada** (HTTP 201):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Laptop Gaming Pro",
  "description": "Laptop de alta gama para gaming con RTX 4080",
  "price": 1599.99,
  "category": "Electronics",
  "stockQuantity": 25,
  "sku": "LAP-GAME-001",
  "active": true,
  "createdAt": "2025-05-19T10:30:00",
  "updatedAt": "2025-05-19T10:30:00"
}
```

### Listar productos (paginado)

```bash
curl http://localhost:8081/api/v1/products?page=0&size=10
```

### Buscar productos

```bash
# Por nombre
curl "http://localhost:8081/api/v1/products/search?query=laptop"

# Por categoría
curl "http://localhost:8081/api/v1/products/search?category=Electronics"

# Por rango de precio
curl "http://localhost:8081/api/v1/products/search?minPrice=100&maxPrice=2000"

# Combinado
curl "http://localhost:8081/api/v1/products/search?query=laptop&category=Electronics&minPrice=500"
```

### Obtener un producto por ID

```bash
curl http://localhost:8081/api/v1/products/{id}
```

### Actualizar un producto

```bash
curl -X PUT http://localhost:8081/api/v1/products/{id} \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Gaming Pro v2",
    "description": "Versión actualizada con RTX 4090",
    "price": 1899.99,
    "category": "Electronics"
  }'
```

### Eliminar un producto (soft delete)

```bash
curl -X DELETE http://localhost:8081/api/v1/products/{id}
```

### Gestión de Stock

```bash
# Disminuir stock
curl -X PATCH "http://localhost:8081/api/v1/products/{id}/stock/decrease?quantity=5"

# Aumentar stock
curl -X PATCH "http://localhost:8081/api/v1/products/{id}/stock/increase?quantity=10"
```

---

## Reglas de Negocio Implementadas

Estas reglas son las mismas en todas las implementaciones:

| Regla | Descripción |
|---|---|
| Precio positivo | El precio debe ser mayor a 0 |
| Stock no negativo | El stock no puede ser menor a 0 |
| SKU único | No pueden existir dos productos con el mismo SKU |
| Stock insuficiente | No se puede disminuir stock más allá de lo disponible |
| Soft delete | Eliminar un producto lo marca como `active=false` |
| Paginación máxima | El tamaño máximo de página es 100 |

---

## Comparación de Arquitecturas

### Arquitectura Hexagonal (Ports & Adapters)

**Idea central**: El dominio define interfaces (puertos) que son implementadas por adaptadores externos.

```
                    ┌─────────────────┐
   HTTP Request ──→ │  Input Adapter  │ (Controller)
                    │  (Web/REST)     │
                    └────────┬────────┘
                             │ llama
                    ┌────────▼────────┐
                    │   INPUT PORT    │ (Interface)
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  DOMAIN SERVICE │ (Lógica de negocio)
                    │  + DOMAIN MODEL │ (Entidades)
                    └────────┬────────┘
                             │ usa
                    ┌────────▼────────┐
                    │  OUTPUT PORT    │ (Interface)
                    └────────┬────────┘
                             │ implementado por
                    ┌────────▼────────┐
                    │ Output Adapter  │ (Repository/DB)
                    └─────────────────┘
```

**Ventajas**:
- El dominio es 100% independiente del framework
- Fácil de testear (mock de puertos)
- Puedes cambiar la base de datos sin tocar el dominio

**Cuándo usarla**: Proyectos medianos-grandes donde la testabilidad y flexibilidad son prioritarias.

---

### Arquitectura por Capas (Layered)

**Idea central**: El código se organiza en capas horizontales con dependencias que fluyen hacia abajo.

```
    ┌─────────────────────────────────┐
    │     PRESENTATION LAYER          │  Controllers, DTOs
    │     (Capa de Presentación)      │
    └────────────────┬────────────────┘
                     │ depende de
    ┌────────────────▼────────────────┐
    │       BUSINESS LAYER            │  Services, Validaciones
    │     (Capa de Negocio)           │
    └────────────────┬────────────────┘
                     │ depende de
    ┌────────────────▼────────────────┐
    │         DATA LAYER              │  Repositories, Entities
    │       (Capa de Datos)           │
    └─────────────────────────────────┘
```

**Ventajas**:
- Simple y fácil de entender
- Menos código boilerplate
- Ideal para equipos con menos experiencia

**Desventajas**:
- Mayor acoplamiento entre capas
- El servicio depende directamente de la implementación del repositorio

**Cuándo usarla**: Proyectos pequeños-medianos, MVPs, cuando la simplicidad es más importante que la flexibilidad.

---

### Arquitectura Limpia (Clean Architecture)

**Idea central**: Las dependencias siempre apuntan hacia adentro. Las capas internas no conocen las externas.

```
    ┌─────────────────────────────────────────────┐
    │  FRAMEWORKS & DRIVERS (más externa)         │
    │  Express, Spring, EF Core, PostgreSQL       │
    │  ┌─────────────────────────────────────┐    │
    │  │  INTERFACE ADAPTERS                 │    │
    │  │  Controllers, Gateways, Presenters  │    │
    │  │  ┌─────────────────────────────┐    │    │
    │  │  │  USE CASES                  │    │    │
    │  │  │  Un caso de uso por clase   │    │    │
    │  │  │  ┌─────────────────────┐    │    │    │
    │  │  │  │  ENTITIES           │    │    │    │
    │  │  │  │  (más interna)      │    │    │    │
    │  │  │  │  Reglas de negocio  │    │    │    │
    │  │  │  └─────────────────────┘    │    │    │
    │  │  └─────────────────────────────┘    │    │
    │  └─────────────────────────────────────┘    │
    └─────────────────────────────────────────────┘
    
    Dependencias: siempre de afuera → hacia adentro
```

**Ventajas**:
- Máxima independencia del framework
- Un caso de uso = una clase (Single Responsibility)
- Muy testeable
- Ideal para sistemas de larga vida

**Desventajas**:
- Más clases y archivos
- Mayor curva de aprendizaje
- Puede ser excesivo para proyectos simples

**Cuándo usarla**: Proyectos grandes y complejos, sistemas enterprise, cuando el negocio es lo más importante.

---

## Tabla Comparativa Rápida

| Aspecto | Hexagonal | Layered | Clean |
|---|---|---|---|
| Complejidad | Media | Baja | Alta |
| Testabilidad | Alta | Media | Muy Alta |
| Acoplamiento | Bajo | Alto | Muy Bajo |
| Cantidad de código | Media | Baja | Alta |
| Independencia de framework | Sí | No | Sí |
| Curva de aprendizaje | Media | Baja | Alta |
| Ideal para | Proyectos medianos | MVPs, proyectos pequeños | Sistemas enterprise |

---

## Ejecutar sin Docker (Desarrollo Local)

Si prefieres ejecutar sin Docker (usa base de datos en memoria):

### Java

```bash
cd java-mvc/hexagonal-architecture
mvn spring-boot:run
# Usa H2 in-memory por defecto
```

### .NET

```bash
cd dotnet-mvc/HexagonalArchitecture
dotnet run
# Usa EF Core InMemory por defecto
```

### Node.js

```bash
cd nodejs-mvc/hexagonal-architecture
npm install
npm run dev
# Usa repositorio in-memory por defecto
```

### Python

```bash
cd python-mvc/hexagonal-architecture
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8081 --reload
# Usa repositorio in-memory por defecto (USE_POSTGRES=false)
```

---

## Ejecutar Tests

### Java

```bash
cd java-mvc/hexagonal-architecture
mvn test
```

### .NET

```bash
cd dotnet-mvc/HexagonalArchitecture
dotnet test
```

### Node.js

```bash
cd nodejs-mvc/hexagonal-architecture
npm install
npm test
```

### Python

```bash
cd python-mvc/hexagonal-architecture
pip install -r requirements.txt
pytest tests/ -v
```

---

## Solución de Problemas Comunes

### El contenedor no arranca

**Síntoma**: El servicio se reinicia constantemente.

**Solución**: Verifica que PostgreSQL esté listo antes de que la app intente conectarse:
```bash
docker compose logs postgres
```
Si PostgreSQL no arranca, puede ser un conflicto de puertos:
```bash
# Verificar qué usa el puerto
netstat -an | findstr "5432"
```

### Error de conexión a la base de datos

**Síntoma**: `Connection refused` o `ECONNREFUSED`.

**Solución**: 
1. Asegúrate de que el servicio postgres esté healthy:
   ```bash
   docker compose ps
   ```
2. Si acabas de hacer `docker compose up`, espera 10-15 segundos para que PostgreSQL inicialice.

### Puerto ya en uso

**Síntoma**: `Bind for 0.0.0.0:8081 failed: port is already allocated`.

**Solución**:
```bash
# Detener todos los contenedores
docker compose down

# O cambiar el puerto en docker-compose.yml
ports:
  - "9081:8081"  # Usa 9081 externamente
```

### Limpiar todo y empezar de cero

```bash
# Detener y eliminar contenedores, redes y volúmenes
docker compose down -v

# Eliminar imágenes construidas
docker compose down --rmi local

# Reconstruir desde cero
docker compose up --build
```

### Los cambios en el código no se reflejan

Docker construye una imagen con el código al momento del build. Si modificas código:
```bash
docker compose up --build
```

---

## Documentación Swagger/OpenAPI

### Python (FastAPI)
FastAPI genera documentación automáticamente:
- Swagger UI: http://localhost:8081/docs
- ReDoc: http://localhost:8081/redoc

### Java (Spring Boot)
Si SpringDoc está configurado:
- Swagger UI: http://localhost:8081/swagger-ui.html

### .NET (ASP.NET Core)
Si Swagger está habilitado:
- Swagger UI: http://localhost:5081/swagger

---

## Ejercicios Sugeridos

1. **Comparar estructura**: Abre las 3 arquitecturas del mismo lenguaje y compara cómo organizan el mismo código.

2. **Agregar un campo**: Agrega el campo `imageUrl` al producto en las 3 arquitecturas. Nota cuántos archivos debes modificar en cada una.

3. **Cambiar la persistencia**: En la arquitectura hexagonal, crea un nuevo adaptador que use un archivo JSON en lugar de la base de datos. ¿Cuánto código del dominio tuviste que cambiar?

4. **Agregar un caso de uso**: Implementa "Aplicar descuento a un producto" en las 3 arquitecturas. ¿Dónde va la lógica en cada una?

5. **Escribir un test**: Escribe un test unitario para el servicio de productos sin usar la base de datos. ¿En cuál arquitectura es más fácil?

---

## Resumen de Comandos Docker

| Acción | Comando |
|---|---|
| Levantar todo | `docker compose up --build` |
| Levantar en background | `docker compose up --build -d` |
| Ver logs | `docker compose logs -f` |
| Ver logs de un servicio | `docker compose logs -f hexagonal-app` |
| Detener todo | `docker compose down` |
| Detener y borrar datos | `docker compose down -v` |
| Reconstruir un servicio | `docker compose build hexagonal-app` |
| Ver estado | `docker compose ps` |
| Entrar a PostgreSQL | `docker compose exec postgres psql -U postgres` |
