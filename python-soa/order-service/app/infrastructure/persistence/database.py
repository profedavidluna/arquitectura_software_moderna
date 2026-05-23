"""
Database Connection Module - Order Service
============================================
"""

import asyncpg
from app.config import settings


async def create_pool() -> asyncpg.Pool:
    """Create and return an asyncpg connection pool."""
    dsn = (
        f"postgresql://{settings.db_user}:{settings.db_password}"
        f"@{settings.db_host}:{settings.db_port}/{settings.db_name}"
    )
    pool = await asyncpg.create_pool(dsn, min_size=2, max_size=10)
    return pool
