"""
Inventory Service Interface (Protocol)
========================================
Defines the contract for the Inventory Service.

Includes both API operations and saga participant methods.
"""

from typing import Protocol
from uuid import UUID

from app.domain.model import InventoryItem


class IInventoryService(Protocol):
    """
    Protocol defining the Inventory Service contract.

    Includes:
    - CRUD operations for inventory management (REST API)
    - Saga participant methods (invoked by event consumers)
    """

    async def create_inventory(
        self,
        product_id: UUID,
        product_name: str,
        initial_quantity: int,
    ) -> InventoryItem:
        """Create a new inventory entry for a product."""
        ...

    async def get_by_product_id(self, product_id: UUID) -> InventoryItem | None:
        """Get inventory for a specific product."""
        ...

    async def get_all(self) -> list[InventoryItem]:
        """Get all inventory items."""
        ...

    async def add_stock(self, product_id: UUID, quantity: int) -> InventoryItem | None:
        """Add stock to an existing inventory item."""
        ...

    async def reserve_stock(self, order_id: UUID, items: list[dict]) -> bool:
        """
        Saga participant: Attempt to reserve stock for an order.
        Publishes 'stock.reserved' or 'stock.insufficient'.
        """
        ...

    async def release_stock(self, order_id: UUID, items: list[dict]) -> None:
        """
        Saga participant: Release previously reserved stock.
        Called when an order is cancelled (compensation).
        """
        ...
