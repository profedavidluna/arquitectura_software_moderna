"""
Product Service - FastAPI Entry Point
======================================
This module bootstraps the Product Service using FastAPI's lifespan
pattern for proper resource management (database pool, Kafka producer).

SOA Principle: Service Composability
- The service is self-contained with its own lifecycle
- Resources are acquired on startup and released on shutdown
- The lifespan pattern ensures clean resource management
"""

import logging
from contextlib import asynccontextmanager

import asyncpg
from fastapi import FastAPI

from app.config import settings
from app.application.product_service import ProductServiceImpl
from app.infrastructure.persistence.product_repository import ProductRepository
from app.infrastructure.messaging.kafka_producer import KafkaEventPublisher
from app.infrastructure.web.product_router import router, set_service

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Manages the application lifecycle.

    Startup:
        1. Creates PostgreSQL connection pool
        2. Initializes Kafka producer
        3. Wires dependencies (Repository → Service → Router)

    Shutdown:
        1. Stops Kafka producer
        2. Closes database pool

    This pattern follows the Dependency Injection principle (DIP):
    high-level modules don't depend on low-level modules,
    both depend on abstractions.
    """
    logger.info("Starting Product Service on port %d", settings.port)

    # Create database connection pool
    dsn = (
        f"postgresql://{settings.db_user}:{settings.db_password}"
        f"@{settings.db_host}:{settings.db_port}/{settings.db_name}"
    )
    pool = await asyncpg.create_pool(dsn, min_size=2, max_size=10)
    logger.info("Database pool created for %s", settings.db_name)

    # Initialize Kafka producer
    producer = KafkaEventPublisher(settings.kafka_brokers)
    await producer.start()
    logger.info("Kafka producer connected to %s", settings.kafka_brokers)

    # Wire dependencies following DIP
    repository = ProductRepository(pool)
    service = ProductServiceImpl(repository, producer)
    set_service(service)

    logger.info("Product Service started successfully")

    yield

    # Shutdown
    logger.info("Shutting down Product Service...")
    await producer.stop()
    await pool.close()
    logger.info("Product Service stopped")


app = FastAPI(
    title="Product Service",
    description="SOA Product Catalog Management Service",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(router, prefix="/api/products", tags=["Products"])


@app.get("/health")
async def health_check():
    """Health check endpoint for container orchestration."""
    return {"status": "UP", "service": "product-service"}
