"""
Order Service - FastAPI Entry Point
=====================================
Bootstraps the Order Service with Saga orchestration capabilities.

This service:
1. Exposes REST API for order management
2. Publishes order events to Kafka
3. Consumes inventory response events (stock.reserved, stock.insufficient)
4. Orchestrates the distributed order creation saga

Design Pattern: Saga Orchestrator
- The Order Service is the central coordinator
- It initiates the saga by publishing 'order.created'
- It listens for responses and transitions order state accordingly
"""

import asyncio
import logging
from contextlib import asynccontextmanager

import asyncpg
from fastapi import FastAPI

from app.config import settings
from app.application.order_service import OrderServiceImpl
from app.infrastructure.persistence.order_repository import OrderRepository
from app.infrastructure.messaging.kafka_producer import KafkaEventPublisher
from app.infrastructure.messaging.inventory_consumer import InventoryEventConsumer
from app.infrastructure.web.order_router import router, set_service

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Manages the Order Service lifecycle.

    Startup:
        1. Creates PostgreSQL connection pool
        2. Initializes Kafka producer for publishing order events
        3. Starts Kafka consumer for inventory response events
        4. Wires all dependencies

    The consumer runs as a background task, continuously listening
    for stock.reserved and stock.insufficient events from the
    Inventory Service.
    """
    logger.info("Starting Order Service on port %d", settings.port)

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
    repository = OrderRepository(pool)
    service = OrderServiceImpl(repository, producer)
    set_service(service)

    # Start Kafka consumer for inventory responses (Saga participant)
    consumer = InventoryEventConsumer(
        brokers=settings.kafka_brokers,
        group_id="order-service-group",
        service=service,
    )
    await consumer.start()
    logger.info("Inventory event consumer started (Saga listener)")

    logger.info("Order Service started successfully")

    yield

    # Shutdown
    logger.info("Shutting down Order Service...")
    await consumer.stop()
    await producer.stop()
    await pool.close()
    logger.info("Order Service stopped")


app = FastAPI(
    title="Order Service",
    description="SOA Order Management Service with Saga Orchestration",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(router, prefix="/api/orders", tags=["Orders"])


@app.get("/health")
async def health_check():
    """Health check endpoint for container orchestration."""
    return {"status": "UP", "service": "order-service"}
