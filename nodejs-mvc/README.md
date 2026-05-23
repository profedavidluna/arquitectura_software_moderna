# MVC Architecture Patterns - Node.js/TypeScript

## Overview

This module demonstrates three different architecture patterns implementing the same **Product Catalog API** in Node.js with TypeScript. Each implementation uses the same business domain but organizes code differently to highlight architectural trade-offs.

## Architectures Implemented

| Architecture | Port | Key Concept |
|-------------|------|-------------|
| [Hexagonal](./hexagonal-architecture/) | 3081 | Ports & Adapters - domain isolated via interfaces |
| [Layered](./layered-architecture/) | 3082 | Vertical layers with downward dependencies |
| [Clean](./clean-architecture/) | 3083 | Dependency Rule - inner layers independent of outer |

## Quick Start

```bash
# Run all three simultaneously
cd hexagonal-architecture && npm install && npm run dev &
cd layered-architecture && npm install && npm run dev &
cd clean-architecture && npm install && npm run dev &
```

## Architecture Comparison

### Code Organization

| Layer/Concept | Hexagonal | Layered | Clean |
|--------------|-----------|---------|-------|
| Business Entity | `domain/model/` | `data/models/` | `entities/` |
| Business Logic | `domain/service/` | `business/services/` | `usecases/` |
| Data Access Interface | `domain/port/output/` | _(none - concrete)_ | `usecases/interfaces/` |
| Data Access Impl | `adapter/output/` | `data/repositories/` | `frameworks/persistence/` |
| HTTP Controller | `adapter/input/web/` | `presentation/controllers/` | `adapters/controllers/` |
| DTOs | `adapter/input/web/dto/` | `presentation/dto/` | `usecases/dto/` |

### Dependency Direction

- **Hexagonal**: Core ← Adapters (adapters depend on core ports)
- **Layered**: Presentation → Business → Data (top depends on bottom)
- **Clean**: Frameworks → Adapters → Use Cases → Entities (outside depends on inside)

### When to Use Each

| Architecture | Best For |
|-------------|----------|
| Hexagonal | Medium-large apps needing testability and flexibility |
| Layered | Small-medium apps, rapid development, simpler teams |
| Clean | Large enterprise apps, complex business logic, long-lived systems |

## Technology Stack

- **Runtime**: Node.js 18+
- **Language**: TypeScript 5.x
- **Web Framework**: Express 4.x
- **Testing**: Jest + ts-jest
- **Build**: tsc (TypeScript compiler)

## Testing

Each architecture has its own test suite:

```bash
cd hexagonal-architecture && npm test
cd layered-architecture && npm test
cd clean-architecture && npm test
```
