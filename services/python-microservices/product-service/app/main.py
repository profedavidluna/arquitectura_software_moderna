from contextlib import asynccontextmanager

from fastapi import FastAPI
import redis.asyncio as aioredis

from app.config import settings
from app.infrastructure.persistence.database import Database
from app.infrastructure.messaging.producer import KafkaProducer
from app.infrastructure.cache.redis_client import RedisCache
from app.infrastructure.persistence.repository import ProductRepository
from app.application.service import ProductService
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

    repository = ProductRepository(db)
    service = ProductService(repository, producer, cache)

    app.state.db = db
    app.state.producer = producer
    app.state.cache = cache
    app.state.service = service

    yield

    # Shutdown
    await producer.stop()
    await redis_client.close()
    await db.disconnect()


app = FastAPI(
    title="Product Service",
    description="Product catalog microservice",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(create_router(), prefix="/api/products", tags=["products"])


@app.get("/health")
async def health_check():
    return {"status": "UP", "service": settings.app_name}
