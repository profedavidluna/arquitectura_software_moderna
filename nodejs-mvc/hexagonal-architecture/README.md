# Hexagonal Architecture (Ports & Adapters) - Node.js/TypeScript

## Overview

This project implements a **Product Catalog API** using Hexagonal Architecture (also known as Ports & Adapters), demonstrating how to isolate business logic from infrastructure concerns.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    INPUT ADAPTERS                         │
│  ┌─────────────────────────────────────────────────┐    │
│  │  Web Adapter (Express Controller)                │    │
│  │  - ProductController                             │    │
│  └──────────────────────┬──────────────────────────┘    │
│                         │ calls                          │
│  ┌──────────────────────▼──────────────────────────┐    │
│  │           INPUT PORT (Interface)                 │    │
│  │  - ProductServicePort                            │    │
│  └──────────────────────┬──────────────────────────┘    │
│                         │                                │
│  ┌──────────────────────▼──────────────────────────┐    │
│  │              DOMAIN CORE                         │    │
│  │  - Product (Entity)                              │    │
│  │  - ProductService (implements input port)        │    │
│  └──────────────────────┬──────────────────────────┘    │
│                         │ depends on                     │
│  ┌──────────────────────▼──────────────────────────┐    │
│  │          OUTPUT PORT (Interface)                 │    │
│  │  - ProductRepositoryPort                         │    │
│  └──────────────────────┬──────────────────────────┘    │
│                         │ implemented by                 │
│  ┌──────────────────────▼──────────────────────────┐    │
│  │            OUTPUT ADAPTERS                       │    │
│  │  - InMemoryProductRepository                     │    │
│  │  - (could be PostgresProductRepository, etc.)    │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

## Project Structure

```
src/
├── domain/                    # Core business logic (innermost hexagon)
│   ├── model/
│   │   └── Product.ts         # Domain entity with business rules
│   ├── port/
│   │   ├── input/
│   │   │   └── ProductServicePort.ts   # What the app offers (driving port)
│   │   └── output/
│   │       └── ProductRepositoryPort.ts # What the app needs (driven port)
│   └── service/
│       └── ProductService.ts  # Implements input port, uses output port
├── adapter/
│   ├── input/
│   │   └── web/
│   │       ├── ProductController.ts  # HTTP → Domain translation
│   │       └── dto/index.ts          # Request/Response DTOs
│   └── output/
│       └── persistence/
│           └── InMemoryProductRepository.ts  # Implements output port
├── config/
│   └── container.ts           # Dependency injection wiring
└── index.ts                   # Application entry point
```

## Key Principles

1. **Domain Independence**: The domain core has ZERO framework dependencies
2. **Ports as Contracts**: Interfaces define what the domain offers and needs
3. **Adapters as Implementations**: Concrete classes implement ports
4. **Dependency Inversion**: Domain depends on abstractions, not concretions
5. **Testability**: Domain can be tested without any infrastructure

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

## Port: 3081
