from contextlib import asynccontextmanager

from fastapi import FastAPI
import redis.asyncio as aioredis
import httpx

from app.config import settings
from app.infrastructure.persistence.database import Database
from app.infrastructure.messaging.producer import KafkaProducer
from app.infrastructure.cache.redis_client import RedisCache
from app.infrastructure.persistence.repository import CartRepository
from app.infrastructure.web.circuit_breaker import CircuitBreaker
from app.application.service import CartService
from app.infrastructure.web.router import create_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    db = Database(settings.database_url)
    await db.connect()

    producer = KafkaProducer(settings.kafka_bootstrap_servers)
    await producer.start()

    redis_client = aioredis.from_url(settings.redis_url, decode_responses=True)
    cache = RedisCache(redis_client, ttl=settings.cache_ttl)

    http_client = httpx.AsyncClient(timeout=10.0)
    circuit_breaker = CircuitBreaker(name="product-service", threshold=5, timeout=30.0)

    repository = CartRepository(db)
    service = CartService(
        repository=repository,
        producer=producer,
        cache=cache,
        http_client=http_client,
        circuit_breaker=circuit_breaker,
        product_service_url=settings.product_service_url,
    )

    app.state.db = db
    app.state.producer = producer
    app.state.service = service

    yield

    # Shutdown
    await http_client.aclose()
    await producer.stop()
    await redis_client.close()
    await db.disconnect()


app = FastAPI(
    title="Cart Service",
    description="Shopping cart microservice",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(create_router(), prefix="/api/carts", tags=["carts"])


@app.get("/health")
async def health_check():
    return {"status": "UP", "service": settings.app_name}
