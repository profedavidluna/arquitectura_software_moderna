from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import settings
from app.infrastructure.persistence.database import Database
from app.infrastructure.messaging.producer import KafkaProducer
from app.infrastructure.persistence.repository import PaymentRepository
from app.application.service import PaymentService
from app.infrastructure.web.router import create_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    db = Database(settings.database_url)
    await db.connect()

    producer = KafkaProducer(settings.kafka_bootstrap_servers)
    await producer.start()

    repository = PaymentRepository(db)
    service = PaymentService(
        repository=repository, producer=producer,
        max_retries=settings.max_retries,
        retry_base_delay=settings.retry_base_delay,
    )

    app.state.db = db
    app.state.producer = producer
    app.state.service = service

    yield

    # Shutdown
    await producer.stop()
    await db.disconnect()


app = FastAPI(
    title="Payment Service",
    description="Payment processing microservice with retry logic",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(create_router(), prefix="/api/payments", tags=["payments"])


@app.get("/health")
async def health_check():
    return {"status": "UP", "service": settings.app_name}
