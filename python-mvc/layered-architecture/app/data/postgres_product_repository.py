"""
Data Layer: PostgreSQL Product Repository.

Concrete repository implementation using PostgreSQL via asyncpg.
Provides the same API as InMemoryProductRepository for interchangeability.

KEY DIFFERENCE FROM HEXAGONAL ARCHITECTURE:
- Hexagonal: Both InMemory and Postgres repositories implement the same
  abstract port (ProductRepositoryPort). Swapping is done via dependency injection.
- Layered: Both repositories are concrete classes with the same method signatures
  (duck typing). Swapping requires changing the wiring code in main.py.
  There is no formal contract (interface) guaranteeing API compatibility.

This is the trade-off of layered architecture: simpler code, but the "contract"
between layers is implicit rather than explicit.
"""
from __future__ import annotations

from datetime import datetime
from typing import Optional

import asyncpg

from app.data.models import Product


class PostgresProductRepository:
    """
    PostgreSQL product repository.

    Uses asyncpg connection pool for efficient async database access.
    Same API as InMemoryProductRepository (duck typing, no shared interface).
    """

    def __init__(self, pool: asyncpg.Pool) -> None:
        self._pool = pool

    @staticmethod
    def _row_to_product(row: asyncpg.Record) -> Product:
        """Map a database row to a Product model."""
        return Product(
            id=str(row["id"]),
            name=row["name"],
            description=row["description"],
            price=float(row["price"]),
            category=row["category"],
            stock_quantity=row["stock_quantity"],
            sku=row["sku"],
            active=row["active"],
            created_at=row["created_at"],
            updated_at=row["updated_at"],
        )

    async def save(self, product: Product) -> Product:
        """Persist a new product to PostgreSQL."""
        query = """
            INSERT INTO products (id, name, description, price, category,
                                  stock_quantity, sku, active, created_at, updated_at)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
            RETURNING *
        """
        async with self._pool.acquire() as conn:
            row = await conn.fetchrow(
                query,
                product.id,
                product.name,
                product.description,
                product.price,
                product.category,
                product.stock_quantity,
                product.sku,
                product.active,
                product.created_at,
                product.updated_at,
            )
        return self._row_to_product(row)

    async def find_by_id(self, product_id: str) -> Optional[Product]:
        """Find a product by its unique identifier."""
        query = "SELECT * FROM products WHERE id = $1"
        async with self._pool.acquire() as conn:
            row = await conn.fetchrow(query, product_id)
        if row is None:
            return None
        return self._row_to_product(row)

    async def find_all(self, page: int, size: int) -> tuple[list[Product], int]:
        """
        Find all products with pagination.

        Returns a tuple of (products_list, total_count).
        """
        offset = (page - 1) * size

        count_query = "SELECT COUNT(*) FROM products"
        data_query = """
            SELECT * FROM products
            ORDER BY created_at DESC
            LIMIT $1 OFFSET $2
        """

        async with self._pool.acquire() as conn:
            total = await conn.fetchval(count_query)
            rows = await conn.fetch(data_query, size, offset)

        products = [self._row_to_product(row) for row in rows]
        return products, total

    async def find_by_sku(self, sku: str) -> Optional[Product]:
        """Find a product by its SKU."""
        query = "SELECT * FROM products WHERE sku = $1"
        async with self._pool.acquire() as conn:
            row = await conn.fetchrow(query, sku)
        if row is None:
            return None
        return self._row_to_product(row)

    async def search(
        self,
        query: Optional[str] = None,
        category: Optional[str] = None,
        min_price: Optional[float] = None,
        max_price: Optional[float] = None,
    ) -> list[Product]:
        """Search products by various criteria."""
        conditions: list[str] = []
        params: list = []
        param_idx = 1

        if query:
            conditions.append(
                f"(LOWER(name) LIKE ${param_idx} OR LOWER(description) LIKE ${param_idx})"
            )
            params.append(f"%{query.lower()}%")
            param_idx += 1

        if category:
            conditions.append(f"LOWER(category) = ${param_idx}")
            params.append(category.lower())
            param_idx += 1

        if min_price is not None:
            conditions.append(f"price >= ${param_idx}")
            params.append(min_price)
            param_idx += 1

        if max_price is not None:
            conditions.append(f"price <= ${param_idx}")
            params.append(max_price)
            param_idx += 1

        where_clause = " AND ".join(conditions) if conditions else "TRUE"
        sql = f"SELECT * FROM products WHERE {where_clause} ORDER BY name"

        async with self._pool.acquire() as conn:
            rows = await conn.fetch(sql, *params)

        return [self._row_to_product(row) for row in rows]

    async def update(self, product: Product) -> Product:
        """Update an existing product in PostgreSQL."""
        query = """
            UPDATE products
            SET name = $2, description = $3, price = $4, category = $5,
                stock_quantity = $6, sku = $7, active = $8, updated_at = $9
            WHERE id = $1
            RETURNING *
        """
        async with self._pool.acquire() as conn:
            row = await conn.fetchrow(
                query,
                product.id,
                product.name,
                product.description,
                product.price,
                product.category,
                product.stock_quantity,
                product.sku,
                product.active,
                product.updated_at or datetime.utcnow(),
            )
        return self._row_to_product(row)

    async def delete(self, product_id: str) -> None:
        """Delete a product by its identifier."""
        query = "DELETE FROM products WHERE id = $1"
        async with self._pool.acquire() as conn:
            await conn.execute(query, product_id)
