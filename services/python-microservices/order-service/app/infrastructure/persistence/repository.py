import json
from typing import Optional
from decimal import Decimal
from datetime import datetime
from app.domain.models import Order, OrderItem, SagaStep
from app.infrastructure.persistence.database import Database


class OrderRepository:
    def __init__(self, db: Database):
        self.db = db

    async def create(self, order: Order) -> Order:
        query = """
            INSERT INTO orders (id, user_id, status, total_amount, shipping_address, saga_status)
            VALUES ($1, $2, $3, $4, $5, $6) RETURNING *
        """
        row = await self.db.fetch_one(
            query, order.id, order.user_id, order.status,
            order.total_amount,
            json.dumps(order.shipping_address) if order.shipping_address else None,
            order.saga_status,
        )

        # Insert order items
        for item in order.items:
            item.order_id = order.id
            await self.db.execute(
                """INSERT INTO order_items (id, order_id, product_id, product_name, price, quantity)
                   VALUES ($1, $2, $3, $4, $5, $6)""",
                item.id, item.order_id, item.product_id,
                item.product_name, item.price, item.quantity,
            )

        return await self.find_by_id(str(row["id"]))

    async def find_by_id(self, order_id: str) -> Optional[Order]:
        row = await self.db.fetch_one("SELECT * FROM orders WHERE id = $1", order_id)
        if not row:
            return None

        items_rows = await self.db.fetch_all(
            "SELECT * FROM order_items WHERE order_id = $1", order_id
        )
        items = [
            OrderItem(
                id=str(r["id"]), order_id=str(r["order_id"]),
                product_id=str(r["product_id"]), product_name=r["product_name"],
                price=Decimal(str(r["price"])), quantity=r["quantity"],
            )
            for r in items_rows
        ]

        shipping = json.loads(row["shipping_address"]) if row["shipping_address"] else None

        return Order(
            id=str(row["id"]), user_id=str(row["user_id"]),
            status=row["status"], total_amount=Decimal(str(row["total_amount"])),
            shipping_address=shipping, saga_status=row["saga_status"],
            items=items, created_at=row["created_at"], updated_at=row["updated_at"],
        )

    async def find_by_user(self, user_id: str) -> list[Order]:
        rows = await self.db.fetch_all(
            "SELECT id FROM orders WHERE user_id = $1 ORDER BY created_at DESC", user_id
        )
        orders = []
        for row in rows:
            order = await self.find_by_id(str(row["id"]))
            if order:
                orders.append(order)
        return orders

    async def update_status(self, order_id: str, status: str) -> None:
        await self.db.execute(
            "UPDATE orders SET status = $2, updated_at = NOW() WHERE id = $1",
            order_id, status,
        )

    async def update_saga_status(self, order_id: str, saga_status: str) -> None:
        await self.db.execute(
            "UPDATE orders SET saga_status = $2, updated_at = NOW() WHERE id = $1",
            order_id, saga_status,
        )

    async def save_saga_step(self, step: SagaStep) -> SagaStep:
        query = """
            INSERT INTO saga_steps (id, order_id, step_name, status, request_payload, executed_at)
            VALUES ($1, $2, $3, $4, $5, $6) RETURNING *
        """
        await self.db.fetch_one(
            query, step.id, step.order_id, step.step_name, step.status,
            json.dumps(step.request_payload) if step.request_payload else None,
            step.executed_at,
        )
        return step

    async def update_saga_step(self, step_id: str, status: str, response: dict) -> None:
        await self.db.execute(
            "UPDATE saga_steps SET status = $2, response_payload = $3 WHERE id = $1",
            step_id, status, json.dumps(response),
        )
