"""
Application Entry Point: FastAPI Application Setup.

This module is the composition root of the Hexagonal Architecture.
It wires together all the layers:
- Creates the appropriate output adapter (repository)
- Creates the domain service with the repository injected
- Wires the service into the input adapter (controller)
- Configures the FastAPI application

The composition root is the ONLY place where concrete implementations
are instantiated and wired together. This is where Dependency Injection
happens manually (no DI container needed for this scale).
"""
from __future__ import annotations

from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI

from app.adapter.input.web.product_controller import router, set_product_service
from app.adapter.output.persistence.in_memory_product_repository import (
    InMemoryProductRepository,
)
from app.config import settings
from app.domain.service.product_service import ProductService

# Global reference for cleanup
_pool = None


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator:
    """Application lifespan: startup and shutdown logic."""
    global _pool

    if settings.use_postgres:
        import asyncpg

        from app.adapter.output.persistence.postgres_product_repository import (
            PostgresProductRepository,
        )

        # Create connection pool
        _pool = await asyncpg.create_pool(
            host=settings.db_host,
            port=settings.db_port,
            user=settings.db_user,
            password=settings.db_password,
            database=settings.db_name,
            min_size=2,
            max_size=10,
        )

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

    # Wire: Repository -> Service -> Controller
    service = ProductService(repository)
    set_product_service(service)

    yield

    # Shutdown: close pool if using Postgres
    if _pool is not None:
        await _pool.close()


app = FastAPI(
    title="Hexagonal Architecture - Product Catalog",
    description=(
        "Product Catalog API implemented with Hexagonal Architecture "
        "(Ports & Adapters) pattern in Python using FastAPI."
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
        "architecture": "hexagonal",
        "persistence": "postgres" if settings.use_postgres else "in-memory",
    }
