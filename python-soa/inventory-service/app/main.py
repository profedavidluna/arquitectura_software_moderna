"""
Inventory Service - FastAPI Entry Point
=========================================
Bootstraps the Inventory Service with event consumption capabilities.

This service:
1. Exposes REST API for inventory management
2. Consumes order events (order.created, order.cancelled)
3. Publishes inventory response events (stock.reserved, stock.insufficient)
4. Participates in the order creation saga

Design Pattern: Saga Participant
- Reacts to saga commands (order.created)
- Performs local transaction (reserve/release stock)
- Publishes saga response events
"""

import asyncio
import logging
from contextlib import asynccontextmanager

import asyncpg
from fastapi import FastAPI

from app.config import settings
from app.application.inventory_service import InventoryServiceImpl
from app.infrastructure.persistence.inventory_repository import InventoryRepository
from app.infrastructure.messaging.kafka_producer import KafkaEventPublisher
from app.infrastructure.messaging.order_consumer import OrderEventConsumer
from app.infrastructure.web.inventory_router import router, set_service

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Manages the Inventory Service lifecycle.

    Startup:
        1. Creates PostgreSQL connection pool
        2. Initializes Kafka producer for response events
        3. Starts Kafka consumer for order events
        4. Wires all dependencies

    The consumer listens for order.created and order.cancelled events,
    participating in the distributed saga.
    """
    logger.info("Starting Inventory Service on port %d", settings.port)

    # Database pool
    dsn = (
        f"postgresql://{settings.db_user}:{settings.db_password}"
        f"@{settings.db_host}:{settings.db_port}/{settings.db_name}"
    )
    pool = await asyncpg.create_pool(dsn, min_size=2, max_size=10)
    logger.info("Database pool created for %s", settings.db_name)

    # Kafka producer
    producer = KafkaEventPublisher(settings.kafka_brokers)
    await producer.start()
    logger.info("Kafka producer connected")

    # Wire dependencies
    repository = InventoryRepository(pool)
    service = InventoryServiceImpl(repository, producer)
    set_service(service)

    # Start Kafka consumer for order events (Saga participant)
    consumer = OrderEventConsumer(
        brokers=settings.kafka_brokers,
        group_id="inventory-service-group",
        service=service,
    )
    await consumer.start()
    logger.info("Order event consumer started (Saga participant)")

    logger.info("Inventory Service started successfully")

    yield

    # Shutdown
    logger.info("Shutting down Inventory Service...")
    await consumer.stop()
    await producer.stop()
    await pool.close()
    logger.info("Inventory Service stopped")


app = FastAPI(
    title="Inventory Service",
    description="SOA Inventory/Stock Management Service",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(router, prefix="/api/inventory", tags=["Inventory"])


@app.get("/health")
async def health_check():
    """Health check endpoint for container orchestration."""
    return {"status": "UP", "service": "inventory-service"}
