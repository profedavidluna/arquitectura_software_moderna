from contextlib import asynccontextmanager

from fastapi import FastAPI
import httpx

from app.config import settings
from app.infrastructure.persistence.database import Database
from app.infrastructure.messaging.producer import KafkaProducer
from app.infrastructure.persistence.repository import OrderRepository
from app.application.service import OrderService
from app.application.saga import SagaOrchestrator
from app.infrastructure.web.router import create_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    db = Database(settings.database_url)
    await db.connect()

    producer = KafkaProducer(settings.kafka_bootstrap_servers)
    await producer.start()

    http_client = httpx.AsyncClient(timeout=15.0)

    repository = OrderRepository(db)
    saga = SagaOrchestrator(
        http_client=http_client,
        cart_service_url=settings.cart_service_url,
        inventory_service_url=settings.inventory_service_url,
        payment_service_url=settings.payment_service_url,
    )
    service = OrderService(repository, producer, saga)

    app.state.db = db
    app.state.producer = producer
    app.state.service = service

    yield

    # Shutdown
    await http_client.aclose()
    await producer.stop()
    await db.disconnect()


app = FastAPI(
    title="Order Service",
    description="Order management with Saga orchestration",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(create_router(), prefix="/api/orders", tags=["orders"])


@app.get("/health")
async def health_check():
    return {"status": "UP", "service": settings.app_name}
