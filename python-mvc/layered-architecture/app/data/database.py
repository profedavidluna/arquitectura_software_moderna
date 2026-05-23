"""
Data Layer: Database Connection.

Provides async connection pool creation for PostgreSQL using asyncpg.
In Layered Architecture, database concerns live exclusively in the Data Layer.
"""
from __future__ import annotations

import asyncpg

from app.config import settings


async def create_pool() -> asyncpg.Pool:
    """
    Create and return an asyncpg connection pool.

    Uses settings from the application configuration module.
    """
    pool = await asyncpg.create_pool(
        host=settings.db_host,
        port=settings.db_port,
        user=settings.db_user,
        password=settings.db_password,
        database=settings.db_name,
        min_size=2,
        max_size=10,
    )
    return pool
