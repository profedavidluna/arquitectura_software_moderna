"""
Inventory Repository
=====================
Data access layer for InventoryItem entities.

Design Pattern: Repository
- Abstracts database operations for inventory
- Handles mapping between domain objects and database rows
- Provides optimistic concurrency through updated_at checks
"""

import logging
from uuid import UUID

import asyncpg

from app.domain.model import InventoryItem

logger = logging.getLogger(__name__)


class InventoryRepository:
    """Repository for Inventory persistence operations."""

    def __init__(self, pool: asyncpg.Pool):
        self._pool = pool

    async def save(self, item: InventoryItem) -> None:
        """Persist a new inventory item."""
        query = """
            INSERT INTO inventory (id, product_id, product_name, quantity_available, quantity_reserved, updated_at)
            VALUES ($1, $2, $3, $4, $5, $6)
        """
        async with self._pool.acquire() as conn:
            await conn.execute(
                query,
                item.id,
                item.product_id,
                item.product_name,
                item.quantity_available,
                item.quantity_reserved,
                item.updated_at,
            )

    async def find_by_product_id(self, product_id: UUID) -> InventoryItem | None:
        """Find inventory by product ID."""
        query = "SELECT * FROM inventory WHERE product_id = $1"
        async with self._pool.acquire() as conn:
            row = await conn.fetchrow(query, product_id)
            if row:
                return self._map_row(row)
            return None

    async def find_all(self) -> list[InventoryItem]:
        """Retrieve all inventory items."""
        query = "SELECT * FROM inventory ORDER BY product_name"
        async with self._pool.acquire() as conn:
            rows = await conn.fetch(query)
            return [self._map_row(row) for row in rows]

    async def update(self, item: InventoryItem) -> None:
        """Update inventory quantities."""
        query = """
            UPDATE inventory
            SET quantity_available = $2, quantity_reserved = $3, updated_at = $4
            WHERE id = $1
        """
        async with self._pool.acquire() as conn:
            await conn.execute(
                query,
                item.id,
                item.quantity_available,
                item.quantity_reserved,
                item.updated_at,
            )

    async def delete_by_product_id(self, product_id: UUID) -> bool:
        """Delete inventory entry for a product."""
        query = "DELETE FROM inventory WHERE product_id = $1"
        async with self._pool.acquire() as conn:
            result = await conn.execute(query, product_id)
            return result == "DELETE 1"

    @staticmethod
    def _map_row(row: asyncpg.Record) -> InventoryItem:
        """Map a database row to an InventoryItem domain object."""
        return InventoryItem(
            id=row["id"],
            product_id=row["product_id"],
            product_name=row["product_name"] or "",
            quantity_available=row["quantity_available"],
            quantity_reserved=row["quantity_reserved"],
            updated_at=row["updated_at"],
        )
