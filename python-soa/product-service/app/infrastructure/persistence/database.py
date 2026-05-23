"""
Database Connection Module
===========================
Provides asyncpg connection pool creation for PostgreSQL.

Design Pattern: Connection Pool
- Reuses database connections for efficiency
- Manages connection lifecycle automatically
- Provides async context manager for safe usage

SOA Principle: Service Autonomy
- Each service manages its own database connection
- No shared database connections between services
"""

import asyncpg
from app.config import settings


async def create_pool() -> asyncpg.Pool:
    """
    Create and return an asyncpg connection pool.

    The pool manages a set of reusable connections to PostgreSQL,
    reducing the overhead of creating new connections for each query.
    """
    dsn = (
        f"postgresql://{settings.db_user}:{settings.db_password}"
        f"@{settings.db_host}:{settings.db_port}/{settings.db_name}"
    )
    pool = await asyncpg.create_pool(dsn, min_size=2, max_size=10)
    return pool
