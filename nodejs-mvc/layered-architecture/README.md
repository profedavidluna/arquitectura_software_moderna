# Layered Architecture (Vertical Layer) - Node.js/TypeScript

## Overview

This project implements a **Product Catalog API** using traditional Layered Architecture, demonstrating clear separation between Presentation, Business, and Data layers with dependencies flowing downward.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│              PRESENTATION LAYER                           │
│  ┌─────────────────────────────────────────────────┐    │
│  │  ProductController (Express Router)              │    │
│  │  DTOs (Request/Response shapes)                  │    │
│  └──────────────────────┬──────────────────────────┘    │
│                         │ depends on                     │
│  ┌──────────────────────▼──────────────────────────┐    │
│  │              BUSINESS LAYER                      │    │
│  │  ProductService (business logic)                 │    │
│  │  Custom Errors (NotFound, Validation, etc.)      │    │
│  └──────────────────────┬──────────────────────────┘    │
│                         │ depends on                     │
│  ┌──────────────────────▼──────────────────────────┐    │
│  │                DATA LAYER                        │    │
│  │  ProductRepository (data access)                 │    │
│  │  Product (data model)                            │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

## Project Structure

```
src/
├── presentation/              # Top layer - HTTP concerns
│   ├── controllers/
│   │   └── ProductController.ts  # HTTP request handling
│   └── dto/
│       └── index.ts              # Request/Response DTOs
├── business/                  # Middle layer - Business logic
│   ├── services/
│   │   └── ProductService.ts     # Business rules and orchestration
│   └── errors/
│       └── index.ts              # Domain-specific errors
├── data/                      # Bottom layer - Persistence
│   ├── models/
│   │   └── Product.ts            # Data model (shared across layers)
│   └── repositories/
│       └── ProductRepository.ts  # Data access operations
└── index.ts                   # Application entry point
```

## Key Principles

1. **Downward Dependencies**: Presentation → Business → Data (never upward)
2. **Layer Isolation**: Each layer has clear responsibilities
3. **Shared Data Model**: The same model flows through all layers (simpler but more coupled)
4. **Concrete Dependencies**: Business layer depends directly on repository class (no interface)
5. **Simplicity**: Easier to understand than Hexagonal/Clean, good for smaller projects

## Trade-offs vs Hexagonal/Clean

| Aspect | Layered | Hexagonal/Clean |
|--------|---------|-----------------|
| Complexity | Lower | Higher |
| Testability | Good (with DI) | Excellent |
| Coupling | Tighter | Looser |
| Flexibility | Less | More |
| Learning Curve | Easier | Steeper |

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

## Port: 3082
