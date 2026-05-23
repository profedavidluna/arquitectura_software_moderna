import logging
from typing import Optional
from datetime import datetime, timedelta

from app.domain.models import InventoryItem, Reservation
from app.domain.interfaces import InventoryRepositoryProtocol, EventProducerProtocol

logger = logging.getLogger(__name__)


class InsufficientStockError(Exception):
    pass


class InventoryService:
    def __init__(self, repository: InventoryRepositoryProtocol, producer: EventProducerProtocol):
        self.repository = repository
        self.producer = producer

    async def add_stock(self, product_id: str, quantity: int,
                        warehouse_location: Optional[str] = None) -> InventoryItem:
        existing = await self.repository.find_by_product(product_id)
        if existing:
            new_qty = existing.quantity + quantity
            await self.repository.update_quantity(product_id, new_qty)
            existing.quantity = new_qty
            return existing
        else:
            item = InventoryItem(
                product_id=product_id, quantity=quantity,
                warehouse_location=warehouse_location,
            )
            created = await self.repository.create(item)
            await self.producer.publish(
                topic="inventory-events",
                key=product_id,
                value={"event": "STOCK_ADDED", "productId": product_id, "quantity": quantity},
            )
            return created

    async def get_stock(self, product_id: str) -> Optional[InventoryItem]:
        return await self.repository.find_by_product(product_id)

    async def get_all_stock(self) -> list[InventoryItem]:
        return await self.repository.find_all()

    async def reserve_items(self, order_id: str, items: list[dict]) -> list[Reservation]:
        """Reserve stock for an order. Raises InsufficientStockError if not enough stock."""
        reservations = []

        for item in items:
            product_id = item["productId"]
            quantity = item["quantity"]

            inventory = await self.repository.find_by_product(product_id)
            if not inventory or inventory.available < quantity:
                # Rollback previous reservations
                for res in reservations:
                    await self.repository.release_stock(res.product_id, res.quantity)
                    await self.repository.update_reservation_status(res.id, "CANCELLED")
                raise InsufficientStockError(
                    f"Insufficient stock for product {product_id}: "
                    f"available={inventory.available if inventory else 0}, requested={quantity}"
                )

            success = await self.repository.reserve_stock(product_id, quantity)
            if not success:
                raise InsufficientStockError(f"Failed to reserve stock for {product_id}")

            reservation = Reservation(
                order_id=order_id, product_id=product_id, quantity=quantity,
                expires_at=datetime.now() + timedelta(minutes=30),
            )
            created = await self.repository.create_reservation(reservation)
            reservations.append(created)

        await self.producer.publish(
            topic="inventory-events",
            key=order_id,
            value={"event": "STOCK_RESERVED", "orderId": order_id, "items": items},
        )
        return reservations

    async def release_items(self, order_id: str, items: list[dict]) -> None:
        """Release reserved stock (compensation)."""
        for item in items:
            product_id = item["productId"]
            quantity = item["quantity"]
            await self.repository.release_stock(product_id, quantity)

        # Update reservation statuses
        reservations = await self.repository.find_reservations_by_order(order_id)
        for res in reservations:
            await self.repository.update_reservation_status(res.id, "RELEASED")

        await self.producer.publish(
            topic="inventory-events",
            key=order_id,
            value={"event": "STOCK_RELEASED", "orderId": order_id},
        )

    async def confirm_items(self, order_id: str) -> None:
        """Confirm reservation (deduct from stock permanently)."""
        reservations = await self.repository.find_reservations_by_order(order_id)
        for res in reservations:
            await self.repository.confirm_reservation(res.product_id, res.quantity)
            await self.repository.update_reservation_status(res.id, "CONFIRMED")

        await self.producer.publish(
            topic="inventory-events",
            key=order_id,
            value={"event": "STOCK_CONFIRMED", "orderId": order_id},
        )

    async def handle_order_event(self, event: dict) -> None:
        """Handle order events from Kafka."""
        event_type = event.get("event")
        order_id = event.get("orderId")

        if event_type == "ORDER_CONFIRMED":
            await self.confirm_items(order_id)
        elif event_type == "ORDER_CANCELLED":
            items = event.get("items", [])
            await self.release_items(order_id, items)
