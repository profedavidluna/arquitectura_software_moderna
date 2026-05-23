import asyncio
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import settings
from app.infrastructure.messaging.consumer import KafkaConsumer
from app.application.service import NotificationService
from app.infrastructure.web.router import create_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    service = NotificationService()

    consumer = KafkaConsumer(
        bootstrap_servers=settings.kafka_bootstrap_servers,
        topics=[
            "user-events", "product-events", "cart-events",
            "order-events", "payment-events", "inventory-events",
        ],
        group_id="notification-service-group",
        handler=service.handle_event,
    )
    consumer_task = asyncio.create_task(consumer.start())

    app.state.service = service

    yield

    # Shutdown
    await consumer.stop()
    consumer_task.cancel()


app = FastAPI(
    title="Notification Service",
    description="Event-driven notification microservice",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(create_router(), prefix="/api/notifications", tags=["notifications"])


@app.get("/health")
async def health_check():
    return {"status": "UP", "service": settings.app_name}
