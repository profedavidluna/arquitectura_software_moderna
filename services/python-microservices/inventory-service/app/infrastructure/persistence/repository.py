from typing import Optional
from app.domain.models import InventoryItem, Reservation
from app.infrastructure.persistence.database import Database


class InventoryRepository:
    def __init__(self, db: Database):
        self.db = db

    async def create(self, item: InventoryItem) -> InventoryItem:
        query = """
            INSERT INTO inventory (id, product_id, quantity, reserved, warehouse_location)
            VALUES ($1, $2, $3, $4, $5) RETURNING *
        """
        row = await self.db.fetch_one(
            query, item.id, item.product_id, item.quantity,
            item.reserved, item.warehouse_location
        )
        return self._row_to_item(row)

    async def find_by_product(self, product_id: str) -> Optional[InventoryItem]:
        row = await self.db.fetch_one(
            "SELECT * FROM inventory WHERE product_id = $1", product_id
        )
        return self._row_to_item(row) if row else None

    async def find_all(self) -> list[InventoryItem]:
        rows = await self.db.fetch_all("SELECT * FROM inventory ORDER BY product_id")
        return [self._row_to_item(row) for row in rows]

    async def update_quantity(self, product_id: str, quantity: int) -> None:
        await self.db.execute(
            "UPDATE inventory SET quantity = $2, updated_at = NOW() WHERE product_id = $1",
            product_id, quantity,
        )

    async def reserve_stock(self, product_id: str, quantity: int) -> bool:
        result = await self.db.execute(
            """UPDATE inventory SET reserved = reserved + $2, updated_at = NOW()
               WHERE product_id = $1 AND (quantity - reserved) >= $2""",
            product_id, quantity,
        )
        return "UPDATE 1" in result

    async def release_stock(self, product_id: str, quantity: int) -> None:
        await self.db.execute(
            "UPDATE inventory SET reserved = reserved - $2, updated_at = NOW() WHERE product_id = $1",
            product_id, quantity,
        )

    async def confirm_reservation(self, product_id: str, quantity: int) -> None:
        await self.db.execute(
            """UPDATE inventory SET quantity = quantity - $2, reserved = reserved - $2,
               updated_at = NOW() WHERE product_id = $1""",
            product_id, quantity,
        )

    async def create_reservation(self, reservation: Reservation) -> Reservation:
        query = """
            INSERT INTO reservations (id, order_id, product_id, quantity, status, expires_at)
            VALUES ($1, $2, $3, $4, $5, $6) RETURNING *
        """
        row = await self.db.fetch_one(
            query, reservation.id, reservation.order_id, reservation.product_id,
            reservation.quantity, reservation.status, reservation.expires_at,
        )
        return Reservation(
            id=str(row["id"]), order_id=str(row["order_id"]),
            product_id=str(row["product_id"]), quantity=row["quantity"],
            status=row["status"], expires_at=row["expires_at"],
            created_at=row["created_at"],
        )

    async def find_reservations_by_order(self, order_id: str) -> list[Reservation]:
        rows = await self.db.fetch_all(
            "SELECT * FROM reservations WHERE order_id = $1", order_id
        )
        return [
            Reservation(
                id=str(r["id"]), order_id=str(r["order_id"]),
                product_id=str(r["product_id"]), quantity=r["quantity"],
                status=r["status"], expires_at=r["expires_at"],
                created_at=r["created_at"],
            )
            for r in rows
        ]

    async def update_reservation_status(self, reservation_id: str, status: str) -> None:
        await self.db.execute(
            "UPDATE reservations SET status = $2 WHERE id = $1", reservation_id, status
        )

    @staticmethod
    def _row_to_item(row) -> InventoryItem:
        return InventoryItem(
            id=str(row["id"]), product_id=str(row["product_id"]),
            quantity=row["quantity"], reserved=row["reserved"],
            warehouse_location=row["warehouse_location"],
            updated_at=row["updated_at"],
        )
