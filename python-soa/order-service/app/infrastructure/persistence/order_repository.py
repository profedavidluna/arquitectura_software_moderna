"""
Order Repository
=================
Data access layer for Order aggregates.

Design Pattern: Repository with Aggregate Loading
- Loads the complete Order aggregate (Order + OrderItems)
- Maintains consistency of the aggregate boundary
- Uses transactions for multi-table operations
"""

import logging
from decimal import Decimal
from uuid import UUID

import asyncpg

from app.domain.model import Order, OrderItem, OrderStatus

logger = logging.getLogger(__name__)


class OrderRepository:
    """
    Repository for Order persistence operations.

    Handles the Order aggregate which spans two tables:
    'orders' and 'order_items'. Operations use transactions
    to maintain consistency.
    """

    def __init__(self, pool: asyncpg.Pool):
        self._pool = pool

    async def save(self, order: Order) -> None:
        """
        Persist a new order with all its items in a transaction.

        Uses a database transaction to ensure atomicity:
        either the entire order (with items) is saved, or nothing is.
        """
        async with self._pool.acquire() as conn:
            async with conn.transaction():
                # Insert order
                await conn.execute(
                    """
                    INSERT INTO orders (id, user_id, status, total_amount, created_at, updated_at)
                    VALUES ($1, $2, $3, $4, $5, $6)
                    """,
                    order.id,
                    order.user_id,
                    order.status.value,
                    order.total_amount,
                    order.created_at,
                    order.updated_at,
                )

                # Insert order items
                for item in order.items:
                    await conn.execute(
                        """
                        INSERT INTO order_items (id, order_id, product_id, product_name, quantity, unit_price, subtotal)
                        VALUES ($1, $2, $3, $4, $5, $6, $7)
                        """,
                        item.id,
                        item.order_id,
                        item.product_id,
                        item.product_name,
                        item.quantity,
                        item.unit_price,
                        item.subtotal,
                    )

    async def find_by_id(self, order_id: UUID) -> Order | None:
        """
        Find an order by ID, loading the complete aggregate.

        Loads both the order header and all associated items.
        """
        async with self._pool.acquire() as conn:
            # Load order
            order_row = await conn.fetchrow(
                "SELECT * FROM orders WHERE id = $1", order_id
            )
            if not order_row:
                return None

            # Load items
            item_rows = await conn.fetch(
                "SELECT * FROM order_items WHERE order_id = $1", order_id
            )

            return self._map_aggregate(order_row, item_rows)

    async def find_by_user(self, user_id: UUID) -> list[Order]:
        """Find all orders for a specific user."""
        async with self._pool.acquire() as conn:
            order_rows = await conn.fetch(
                "SELECT * FROM orders WHERE user_id = $1 ORDER BY created_at DESC",
                user_id,
            )

            orders = []
            for order_row in order_rows:
                item_rows = await conn.fetch(
                    "SELECT * FROM order_items WHERE order_id = $1",
                    order_row["id"],
                )
                orders.append(self._map_aggregate(order_row, item_rows))

            return orders

    async def update_status(self, order_id: UUID, status: OrderStatus) -> None:
        """Update only the order status (used during saga transitions)."""
        async with self._pool.acquire() as conn:
            await conn.execute(
                """
                UPDATE orders SET status = $2, updated_at = CURRENT_TIMESTAMP
                WHERE id = $1
                """,
                order_id,
                status.value,
            )

    @staticmethod
    def _map_aggregate(
        order_row: asyncpg.Record, item_rows: list[asyncpg.Record]
    ) -> Order:
        """Map database rows to an Order aggregate."""
        items = [
            OrderItem(
                id=row["id"],
                order_id=row["order_id"],
                product_id=row["product_id"],
                product_name=row["product_name"] or "",
                quantity=row["quantity"],
                unit_price=Decimal(str(row["unit_price"])),
                subtotal=Decimal(str(row["subtotal"])),
            )
            for row in item_rows
        ]

        return Order(
            id=order_row["id"],
            user_id=order_row["user_id"],
            status=OrderStatus(order_row["status"]),
            total_amount=Decimal(str(order_row["total_amount"])) if order_row["total_amount"] else Decimal("0"),
            items=items,
            created_at=order_row["created_at"],
            updated_at=order_row["updated_at"],
        )
