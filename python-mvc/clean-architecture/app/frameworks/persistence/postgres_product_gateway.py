"""
@layer Frameworks (Outermost Layer) - PostgreSQL Gateway
@description Implements the ProductGateway interface using asyncpg.

This is the concrete implementation that connects to PostgreSQL.
It implements the interface defined in the use cases layer,
maintaining the Dependency Rule (outer depends on inner).
"""

from decimal import Decimal
from typing import Optional

import asyncpg

from app.entities.product import Product
from app.usecases.interfaces.product_gateway import ProductGateway


class PostgresProductGateway(ProductGateway):
    """
    PostgreSQL implementation of ProductGateway.

    Uses asyncpg connection pooling for high-performance async access.
    Implements the interface defined in the use cases layer.
    """

    def __init__(self, pool: asyncpg.Pool):
        self._pool = pool

    def _row_to_product(self, row: asyncpg.Record) -> Product:
        """Convert a database row to a domain entity."""
        return Product(
            id=str(row["id"]),
            name=row["name"],
            description=row["description"] or "",
            price=Decimal(str(row["price"])),
            category=row["category"] or "",
            stock_quantity=row["stock_quantity"],
            sku=row["sku"],
            active=row["active"],
            created_at=row["created_at"],
            updated_at=row["updated_at"],
        )

    async def save(self, product: Product) -> Product:
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
                float(product.price),
                product.category,
                product.stock_quantity,
                product.sku,
                product.active,
                product.created_at,
                product.updated_at,
            )
        return self._row_to_product(row)

    async def find_by_id(self, product_id: str) -> Optional[Product]:
        query = "SELECT * FROM products WHERE id = $1"
        async with self._pool.acquire() as conn:
            row = await conn.fetchrow(query, product_id)
        return self._row_to_product(row) if row else None

    async def find_all(self, page: int, size: int) -> dict:
        offset = (page - 1) * size

        count_query = "SELECT COUNT(*) FROM products WHERE active = true"
        data_query = """
            SELECT * FROM products
            WHERE active = true
            ORDER BY created_at DESC
            LIMIT $1 OFFSET $2
        """

        async with self._pool.acquire() as conn:
            total = await conn.fetchval(count_query)
            rows = await conn.fetch(data_query, size, offset)

        products = [self._row_to_product(row) for row in rows]
        return {"products": products, "total": total}

    async def find_by_sku(self, sku: str) -> Optional[Product]:
        query = "SELECT * FROM products WHERE sku = $1"
        async with self._pool.acquire() as conn:
            row = await conn.fetchrow(query, sku)
        return self._row_to_product(row) if row else None

    async def search(
        self,
        query: Optional[str] = None,
        category: Optional[str] = None,
        min_price: Optional[Decimal] = None,
        max_price: Optional[Decimal] = None,
    ) -> list[Product]:
        conditions = ["active = true"]
        params = []
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
            params.append(float(min_price))
            param_idx += 1

        if max_price is not None:
            conditions.append(f"price <= ${param_idx}")
            params.append(float(max_price))
            param_idx += 1

        where_clause = " AND ".join(conditions)
        sql = f"SELECT * FROM products WHERE {where_clause} ORDER BY created_at DESC"

        async with self._pool.acquire() as conn:
            rows = await conn.fetch(sql, *params)

        return [self._row_to_product(row) for row in rows]

    async def update(self, product: Product) -> Product:
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
                float(product.price),
                product.category,
                product.stock_quantity,
                product.sku,
                product.active,
                product.updated_at,
            )
        return self._row_to_product(row)

    async def delete(self, product_id: str) -> None:
        query = "DELETE FROM products WHERE id = $1"
        async with self._pool.acquire() as conn:
            await conn.execute(query, product_id)
