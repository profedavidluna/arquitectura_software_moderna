"""
@layer Frameworks (Outermost Layer) - Database Connection
@description Manages the PostgreSQL connection pool.

This is a framework concern - it deals with the specific technology
(asyncpg) used to connect to PostgreSQL. Inner layers know nothing
about this; they only know about the ProductGateway interface.
"""

from typing import Optional

import asyncpg

from app.config import get_config


class Database:
    """PostgreSQL connection pool manager."""

    def __init__(self):
        self._pool: Optional[asyncpg.Pool] = None

    @property
    def pool(self) -> asyncpg.Pool:
        if self._pool is None:
            raise RuntimeError("Database not initialized")
        return self._pool

    async def connect(self) -> None:
        """Create the connection pool."""
        config = get_config()
        self._pool = await asyncpg.create_pool(
            host=config.host,
            port=config.port,
            user=config.user,
            password=config.password,
            database=config.database,
            min_size=2,
            max_size=10,
        )

    async def disconnect(self) -> None:
        """Close the connection pool."""
        if self._pool:
            await self._pool.close()
            self._pool = None
