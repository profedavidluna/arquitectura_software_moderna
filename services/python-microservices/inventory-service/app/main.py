import asyncio
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import settings
from app.infrastructure.persistence.database import Database
from app.infrastructure.messaging.producer import KafkaProducer
from app.infrastructure.messaging.consumer import KafkaConsumer
from app.infrastructure.persistence.repository import InventoryRepository
from app.application.service import InventoryService
from app.infrastructure.web.router import create_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    db = Database(settings.database_url)
    await db.connect()

    producer = KafkaProducer(settings.kafka_bootstrap_servers)
    await producer.start()

    repository = InventoryRepository(db)
    service = InventoryService(repository, producer)

    consumer = KafkaConsumer(
        bootstrap_servers=settings.kafka_bootstrap_servers,
        topics=["order-events"],
        group_id="inventory-service-group",
        handler=service.handle_order_event,
    )
    consumer_task = asyncio.create_task(consumer.start())

    app.state.db = db
    app.state.producer = producer
    app.state.service = service

    yield

    # Shutdown
    await consumer.stop()
    consumer_task.cancel()
    await producer.stop()
    await db.disconnect()


app = FastAPI(
    title="Inventory Service",
    description="Stock management microservice",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(create_router(), prefix="/api/inventory", tags=["inventory"])


@app.get("/health")
async def health_check():
    return {"status": "UP", "service": settings.app_name}
