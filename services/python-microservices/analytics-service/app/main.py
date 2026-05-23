import asyncio
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import settings
from app.infrastructure.persistence.database import Database
from app.infrastructure.messaging.consumer import KafkaConsumer
from app.infrastructure.persistence.repository import AnalyticsRepository
from app.application.service import AnalyticsService
from app.infrastructure.web.router import create_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    db = Database(settings.database_url)
    await db.connect()

    repository = AnalyticsRepository(db)
    service = AnalyticsService(repository)

    consumer = KafkaConsumer(
        bootstrap_servers=settings.kafka_bootstrap_servers,
        topics=[
            "user-events", "product-events", "cart-events",
            "order-events", "payment-events", "inventory-events",
        ],
        group_id="analytics-service-group",
        handler=service.handle_event,
    )
    consumer_task = asyncio.create_task(consumer.start())

    app.state.db = db
    app.state.service = service

    yield

    # Shutdown
    await consumer.stop()
    consumer_task.cancel()
    await db.disconnect()


app = FastAPI(
    title="Analytics Service",
    description="Event aggregation and metrics microservice",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(create_router(), prefix="/api/analytics", tags=["analytics"])


@app.get("/health")
async def health_check():
    return {"status": "UP", "service": settings.app_name}
