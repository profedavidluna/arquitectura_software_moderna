"""
Inventory Service Implementation
===================================
Implements stock management and saga participation.

Design Pattern: Saga Participant
================================
This service participates in the order creation saga:

1. Receives 'order.created' event
2. Attempts to reserve stock for ALL items in the order
3. If ALL items can be reserved:
   - Reserves stock (available → reserved)
   - Publishes 'stock.reserved' event
4. If ANY item cannot be reserved:
   - Rolls back any partial reservations
   - Publishes 'stock.insufficient' event

5. On 'order.cancelled' event:
   - Releases all reserved stock (reserved → available)
   - Publishes 'stock.released' event

This ensures atomicity within the service boundary while
maintaining eventual consistency across the distributed system.
"""

import logging
from uuid import UUID, uuid4

from app.domain.model import InventoryItem
from app.infrastructure.persistence.inventory_repository import InventoryRepository
from app.infrastructure.messaging.kafka_producer import KafkaEventPublisher
from app.infrastructure.messaging.events import InventoryEvents

logger = logging.getLogger(__name__)


class InventoryServiceImpl:
    """
    Inventory Service implementation with saga participation.

    Manages stock levels and coordinates with the Order Service
    through Kafka events as part of the distributed saga.
    """

    def __init__(self, repository: InventoryRepository, publisher: KafkaEventPublisher):
        self._repository = repository
        self._publisher = publisher

    async def create_inventory(
        self,
        product_id: UUID,
        product_name: str,
        initial_quantity: int = 0,
    ) -> InventoryItem:
        """Create a new inventory entry for a product."""
        item = InventoryItem(
            id=uuid4(),
            product_id=product_id,
            product_name=product_name,
            quantity_available=initial_quantity,
            quantity_reserved=0,
        )

        await self._repository.save(item)
        logger.info(
            "Inventory created for product %s: %d units available",
            product_id, initial_quantity,
        )
        return item

    async def get_by_product_id(self, product_id: UUID) -> InventoryItem | None:
        """Get inventory for a specific product."""
        return await self._repository.find_by_product_id(product_id)

    async def get_all(self) -> list[InventoryItem]:
        """Get all inventory items."""
        return await self._repository.find_all()

    async def add_stock(self, product_id: UUID, quantity: int) -> InventoryItem | None:
        """Add stock to an existing inventory item."""
        item = await self._repository.find_by_product_id(product_id)
        if not item:
            return None

        item.add_stock(quantity)
        await self._repository.update(item)
        logger.info(
            "Added %d units to product %s (now: %d available)",
            quantity, product_id, item.quantity_available,
        )
        return item

    async def reserve_stock(self, order_id: UUID, items: list[dict]) -> bool:
        """
        Saga participant: Attempt to reserve stock for an order.

        This is the core saga logic:
        1. Check if ALL items have sufficient stock
        2. If yes: reserve all and publish 'stock.reserved'
        3. If no: rollback partial reservations and publish 'stock.insufficient'

        The all-or-nothing approach ensures order consistency.
        """
        reserved_items: list[tuple[InventoryItem, int]] = []

        try:
            # Phase 1: Validate and reserve all items
            for item_data in items:
                product_id = UUID(item_data["product_id"])
                quantity = item_data["quantity"]

                inventory = await self._repository.find_by_product_id(product_id)

                if not inventory:
                    raise ValueError(
                        f"No inventory found for product {product_id}"
                    )

                if not inventory.can_reserve(quantity):
                    raise ValueError(
                        f"Insufficient stock for product {product_id}: "
                        f"requested {quantity}, available {inventory.quantity_available}"
                    )

                # Reserve stock
                inventory.reserve(quantity)
                await self._repository.update(inventory)
                reserved_items.append((inventory, quantity))

            # Phase 2: All reservations successful - publish success event
            event = InventoryEvents.stock_reserved(order_id, items)
            await self._publisher.publish("stock.reserved", str(order_id), event)
            logger.info("Stock reserved for order %s - saga continues", order_id)
            return True

        except ValueError as e:
            # Phase 3: Rollback partial reservations (compensation within service)
            logger.warning(
                "Cannot reserve stock for order %s: %s - rolling back",
                order_id, e,
            )

            for inventory, quantity in reserved_items:
                inventory.release(quantity)
                await self._repository.update(inventory)

            # Publish failure event
            event = InventoryEvents.stock_insufficient(order_id, str(e))
            await self._publisher.publish("stock.insufficient", str(order_id), event)
            logger.info("Published stock.insufficient for order %s", order_id)
            return False

    async def release_stock(self, order_id: UUID, items: list[dict]) -> None:
        """
        Saga participant: Release reserved stock (compensation).

        Called when an order is cancelled after stock was reserved.
        Moves stock from reserved back to available.
        """
        for item_data in items:
            product_id = UUID(item_data["product_id"])
            quantity = item_data["quantity"]

            inventory = await self._repository.find_by_product_id(product_id)
            if inventory:
                inventory.release(quantity)
                await self._repository.update(inventory)
                logger.info(
                    "Released %d units for product %s (order: %s)",
                    quantity, product_id, order_id,
                )

        # Publish stock released event
        event = InventoryEvents.stock_released(order_id, items)
        await self._publisher.publish("stock.released", str(order_id), event)
        logger.info("Stock released for cancelled order %s", order_id)
