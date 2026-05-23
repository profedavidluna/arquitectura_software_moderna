"""
Application Entry Point: FastAPI Application Setup.

This module wires together all layers of the Layered Architecture:
    Presentation Layer → Business Layer → Data Layer

LAYERED ARCHITECTURE OVERVIEW:
┌─────────────────────────────────────────────┐
│  Presentation Layer (Controllers, DTOs)     │  ← HTTP interface
├─────────────────────────────────────────────┤
│  Business Layer (Services, Errors)          │  ← Business logic
├─────────────────────────────────────────────┤
│  Data Layer (Repositories, Models, DB)      │  ← Data access
└─────────────────────────────────────────────┘

KEY DIFFERENCES FROM HEXAGONAL ARCHITECTURE:
- Hexagonal: Uses ports (interfaces) and adapters. The domain core is
  completely isolated. Dependencies point inward via Dependency Inversion.
- Layered: Dependencies flow top-down (Presentation → Business → Data).
  No interfaces/ports — concrete classes depend on each other directly.
  Simpler to understand but more coupled.

WIRING:
- At startup, we create the appropriate repository (in-memory or Postgres)
- Pass it directly to the ProductService (no interface indirection)
- Pass the service to the controller module
- Each layer only calls the layer directly below it
"""
from __future__ import annotations

from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI

from app.config import settings
from app.business.product_service import ProductService
from app.data.product_repository import InMemoryProductRepository
from app.presentation.product_controller import router, set_product_service

# Global reference for cleanup
_pool = None


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator:
    """Application lifespan: startup and shutdown logic."""
    global _pool

    if settings.use_postgres:
        from app.data.database import create_pool
        from app.data.postgres_product_repository import PostgresProductRepository

        # Create connection pool
        _pool = await create_pool()

        # Create table if not exists
        async with _pool.acquire() as conn:
            await conn.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    price DECIMAL(10, 2) NOT NULL,
                    category VARCHAR(100) NOT NULL,
                    stock_quantity INTEGER NOT NULL DEFAULT 0,
                    sku VARCHAR(50) UNIQUE NOT NULL,
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT NOW(),
                    updated_at TIMESTAMP DEFAULT NOW()
                )
            """)

        repository = PostgresProductRepository(_pool)
    else:
        repository = InMemoryProductRepository()

    # Wire layers: Data → Business → Presentation
    service = ProductService(repository)
    set_product_service(service)

    yield

    # Shutdown: close pool if using Postgres
    if _pool is not None:
        await _pool.close()


app = FastAPI(
    title="Layered Architecture - Product Catalog",
    description=(
        "Product Catalog API implemented with Layered Architecture "
        "(Presentation → Business → Data) pattern in Python using FastAPI."
    ),
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(router)


@app.get("/health", tags=["Health"])
async def health_check() -> dict:
    """Health check endpoint."""
    return {
        "status": "healthy",
        "service": "product-catalog",
        "architecture": "layered",
        "persistence": "postgres" if settings.use_postgres else "in-memory",
    }
