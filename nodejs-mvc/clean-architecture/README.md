# Clean Architecture - Node.js/TypeScript

## Overview

This project implements a **Product Catalog API** using Clean Architecture, demonstrating the Dependency Rule where source code dependencies always point inward toward higher-level policies.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│  FRAMEWORKS & DRIVERS (Outermost)                                │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Express App, InMemoryProductGateway                       │  │
│  └────────────────────────────┬──────────────────────────────┘  │
│                               │                                  │
│  INTERFACE ADAPTERS           │                                  │
│  ┌────────────────────────────▼──────────────────────────────┐  │
│  │  ProductController (HTTP → Use Case input)                 │  │
│  └────────────────────────────┬──────────────────────────────┘  │
│                               │                                  │
│  USE CASES (Application Business Rules)                          │
│  ┌────────────────────────────▼──────────────────────────────┐  │
│  │  CreateProductUseCase, GetProductUseCase,                  │  │
│  │  ListProductsUseCase, SearchProductsUseCase,               │  │
│  │  UpdateProductUseCase, DeleteProductUseCase,                │  │
│  │  ManageStockUseCase                                        │  │
│  │  + ProductGateway interface (defined here)                 │  │
│  └────────────────────────────┬──────────────────────────────┘  │
│                               │                                  │
│  ENTITIES (Enterprise Business Rules - Innermost)                │
│  ┌────────────────────────────▼──────────────────────────────┐  │
│  │  Product (domain entity with business invariants)          │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

Dependencies always point INWARD →
```

## Project Structure

```
src/
├── entities/                  # Innermost - Enterprise Business Rules
│   └── Product.ts             # Domain entity with invariants
├── usecases/                  # Application Business Rules
│   ├── interfaces/
│   │   └── ProductGateway.ts  # Gateway interface (Dependency Rule)
│   ├── dto/
│   │   └── ProductDTO.ts      # Input/Output data structures
│   ├── errors/
│   │   └── UseCaseErrors.ts   # Application-specific errors
│   ├── CreateProductUseCase.ts
│   ├── GetProductUseCase.ts
│   ├── ListProductsUseCase.ts
│   ├── SearchProductsUseCase.ts
│   ├── UpdateProductUseCase.ts
│   ├── DeleteProductUseCase.ts
│   └── ManageStockUseCase.ts
├── adapters/                  # Interface Adapters
│   └── controllers/
│       └── ProductController.ts  # HTTP → Use Case translation
├── frameworks/                # Frameworks & Drivers (Outermost)
│   ├── persistence/
│   │   └── InMemoryProductGateway.ts  # Implements gateway interface
│   └── web/
│       └── ExpressApp.ts      # Express setup
└── index.ts                   # Composition Root (wires everything)
```

## Key Principles

1. **Dependency Rule**: Dependencies point inward. Inner layers know nothing about outer layers.
2. **Entities**: Contain enterprise-wide business rules (validation, invariants)
3. **Use Cases**: Contain application-specific business rules (orchestration)
4. **Interface Adapters**: Convert data between use case format and external format
5. **Frameworks & Drivers**: Concrete implementations of interfaces defined by inner layers
6. **Single Responsibility**: Each use case class has exactly one job

## Differences from Hexagonal Architecture

| Aspect | Clean Architecture | Hexagonal |
|--------|-------------------|-----------|
| Use Cases | Separate classes per operation | Single service class |
| Layers | 4 explicit layers | 3 zones (core, ports, adapters) |
| Entity | Rich domain object with factory | Domain model |
| Gateway | Interface in use case layer | Port in domain layer |
| Focus | Dependency Rule | Port/Adapter metaphor |

## Running

```bash
npm install
npm run dev     # Development with ts-node
npm run build   # Compile TypeScript
npm start       # Run compiled JS
npm test        # Run tests with coverage
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/products | Create product |
| GET | /api/v1/products | List products (paginated) |
| GET | /api/v1/products/:id | Get product by ID |
| GET | /api/v1/products/search | Search products |
| PUT | /api/v1/products/:id | Update product |
| DELETE | /api/v1/products/:id | Soft delete product |
| PATCH | /api/v1/products/:id/stock/decrease | Decrease stock |
| PATCH | /api/v1/products/:id/stock/increase | Increase stock |

## Port: 3083
