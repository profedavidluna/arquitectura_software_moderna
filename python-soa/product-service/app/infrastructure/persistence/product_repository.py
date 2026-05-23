"""
Product Repository
===================
Data access layer for Product entities using asyncpg.

Design Pattern: Repository
- Abstracts data access logic from business logic
- Provides a collection-like interface for domain objects
- Handles the mapping between domain models and database rows

SOLID Principle: Single Responsibility (SRP)
- Only responsible for Product data persistence
- SQL queries are encapsulated here, not in the service layer
"""

import logging
from decimal import Decimal
from uuid import UUID

import asyncpg

from app.domain.model import Product

logger = logging.getLogger(__name__)


class ProductRepository:
    """
    Repository for Product persistence operations.

    Uses asyncpg connection pool for efficient async database access.
    Maps between Product domain objects and PostgreSQL rows.
    """

    def __init__(self, pool: asyncpg.Pool):
        self._pool = pool

    async def save(self, product: Product) -> None:
        """Persist a new product to the database."""
        query = """
            INSERT INTO products (id, name, description, price, category, sku, active, created_at, updated_at)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
        """
        async with self._pool.acquire() as conn:
            await conn.execute(
                query,
                product.id,
                product.name,
                product.description,
                product.price,
                product.category,
                product.sku,
                product.active,
                product.created_at,
                product.updated_at,
            )

    async def find_by_id(self, product_id: UUID) -> Product | None:
        """Find a product by its UUID. Returns None if not found."""
        query = "SELECT * FROM products WHERE id = $1"
        async with self._pool.acquire() as conn:
            row = await conn.fetchrow(query, product_id)
            if row:
                return self._map_row(row)
            return None

    async def find_all(self) -> list[Product]:
        """Retrieve all active products ordered by creation date."""
        query = "SELECT * FROM products WHERE active = true ORDER BY created_at DESC"
        async with self._pool.acquire() as conn:
            rows = await conn.fetch(query)
            return [self._map_row(row) for row in rows]

    async def find_by_sku(self, sku: str) -> Product | None:
        """Find a product by its SKU (Stock Keeping Unit)."""
        query = "SELECT * FROM products WHERE sku = $1"
        async with self._pool.acquire() as conn:
            row = await conn.fetchrow(query, sku)
            if row:
                return self._map_row(row)
            return None

    async def update(self, product: Product) -> None:
        """Update an existing product in the database."""
        query = """
            UPDATE products
            SET name = $2, description = $3, price = $4, category = $5,
                active = $6, updated_at = $7
            WHERE id = $1
        """
        async with self._pool.acquire() as conn:
            await conn.execute(
                query,
                product.id,
                product.name,
                product.description,
                product.price,
                product.category,
                product.active,
                product.updated_at,
            )

    async def delete(self, product_id: UUID) -> bool:
        """Hard delete a product (use with caution)."""
        query = "DELETE FROM products WHERE id = $1"
        async with self._pool.acquire() as conn:
            result = await conn.execute(query, product_id)
            return result == "DELETE 1"

    @staticmethod
    def _map_row(row: asyncpg.Record) -> Product:
        """Map a database row to a Product domain object."""
        return Product(
            id=row["id"],
            name=row["name"],
            description=row["description"],
            price=Decimal(str(row["price"])),
            category=row["category"],
            sku=row["sku"],
            active=row["active"],
            created_at=row["created_at"],
            updated_at=row["updated_at"],
        )
