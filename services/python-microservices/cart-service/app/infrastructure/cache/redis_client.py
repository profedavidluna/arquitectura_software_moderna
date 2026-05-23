import logging
from typing import Optional
from redis.asyncio import Redis

logger = logging.getLogger(__name__)


class RedisCache:
    def __init__(self, client: Redis, ttl: int = 300):
        self.client = client
        self.ttl = ttl

    async def get(self, key: str) -> Optional[str]:
        try:
            return await self.client.get(key)
        except Exception as e:
            logger.warning(f"Redis GET failed for {key}: {e}")
            return None

    async def set(self, key: str, value: str, ttl: Optional[int] = None) -> None:
        try:
            await self.client.set(key, value, ex=ttl or self.ttl)
        except Exception as e:
            logger.warning(f"Redis SET failed for {key}: {e}")

    async def delete(self, key: str) -> None:
        try:
            await self.client.delete(key)
        except Exception as e:
            logger.warning(f"Redis DELETE failed for {key}: {e}")
