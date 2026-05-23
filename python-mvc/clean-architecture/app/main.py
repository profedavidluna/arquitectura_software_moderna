"""
@layer Frameworks (Application Entry Point)
@description FastAPI application setup and dependency wiring for Clean Architecture.

This is the COMPOSITION ROOT - the only place that knows about ALL layers.
It wires together:
1. The gateway implementation (frameworks layer)
2. The use cases (use cases layer, receiving the gateway)
3. The controller (adapters layer, receiving use cases)

In Clean Architecture, the main module is in the outermost layer because
it needs to know about everything to wire dependencies together.
This is the only file that violates the Dependency Rule by necessity.
"""

from contextlib import asynccontextmanager

import asyncpg
from fastapi import FastAPI

from app.config import get_config
from app.adapters.controllers import product_controller
from app.frameworks.persistence.in_memory_product_gateway import InMemoryProductGateway
from app.frameworks.persistence.postgres_product_gateway import PostgresProductGateway
from app.usecases.create_product import CreateProductUseCase
from app.usecases.get_product import GetProductUseCase
from app.usecases.list_products import ListProductsUseCase
from app.usecases.search_products import SearchProductsUseCase
from app.usecases.update_product import UpdateProductUseCase
from app.usecases.delete_product import DeleteProductUseCase
from app.usecases.manage_stock import DecreaseStockUseCase, IncreaseStockUseCase


_pool: asyncpg.Pool = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan - wires all dependencies together.

    This is where the Dependency Rule is enforced at runtime:
    - Gateway (frameworks) implements the interface defined in use cases
    - Use cases receive the gateway via constructor injection
    - Controller receives use cases via injection
    """
    global _pool
    config = get_config()

    # 1. Create the gateway (frameworks layer)
    if config.use_postgres:
        _pool = await asyncpg.create_pool(
            host=config.host,
            port=config.port,
            user=config.user,
            password=config.password,
            database=config.database,
            min_size=2,
            max_size=10,
        )
        gateway = PostgresProductGateway(_pool)
        print(f"✓ Connected to PostgreSQL at {config.host}:{config.port}/{config.database}")
    else:
        gateway = InMemoryProductGateway()
        print("✓ Using in-memory gateway (no database)")

    # 2. Create use cases (each receives the gateway)
    create_product = CreateProductUseCase(gateway)
    get_product = GetProductUseCase(gateway)
    list_products = ListProductsUseCase(gateway)
    search_products = SearchProductsUseCase(gateway)
    update_product = UpdateProductUseCase(gateway)
    delete_product = DeleteProductUseCase(gateway)
    decrease_stock = DecreaseStockUseCase(gateway)
    increase_stock = IncreaseStockUseCase(gateway)

    # 3. Inject use cases into the controller (adapters layer)
    product_controller.set_use_cases(
        create=create_product,
        get=get_product,
        list_all=list_products,
        search=search_products,
        update=update_product,
        delete=delete_product,
        decrease=decrease_stock,
        increase=increase_stock,
    )

    yield

    # Cleanup
    if _pool:
        await _pool.close()
        print("✓ PostgreSQL connection pool closed")


# Create FastAPI application
app = FastAPI(
    title="Product Catalog API - Clean Architecture",
    description=(
        "Implementation using Clean Architecture (Uncle Bob). "
        "One use case per class, strict Dependency Rule."
    ),
    version="1.0.0",
    lifespan=lifespan,
)

# Register controller routes
app.include_router(product_controller.router)


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "architecture": "clean"}
