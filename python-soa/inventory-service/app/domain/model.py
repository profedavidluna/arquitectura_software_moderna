"""
Inventory Domain Model
========================
Core business entity for stock management.

Business Rules:
- quantity_available: Stock that can be sold
- quantity_reserved: Stock reserved for pending orders
- Total physical stock = quantity_available + quantity_reserved
- Reservation moves stock from available to reserved
- Release moves stock from reserved back to available
- Cannot reserve more than available
"""

from dataclasses import dataclass, field
from datetime import datetime
from uuid import UUID


@dataclass
class InventoryItem:
    """
    Represents the inventory/stock level for a product.

    The inventory uses a two-phase approach:
    1. Reserve: Move stock from available to reserved (during order creation)
    2. Confirm: Deduct from reserved (when order ships) - not implemented in this demo
    3. Release: Move stock from reserved back to available (on cancellation)

    This prevents overselling while allowing concurrent order processing.
    """

    id: UUID
    product_id: UUID
    product_name: str
    quantity_available: int = 0
    quantity_reserved: int = 0
    updated_at: datetime = field(default_factory=datetime.utcnow)

    @property
    def total_stock(self) -> int:
        """Total physical stock (available + reserved)."""
        return self.quantity_available + self.quantity_reserved

    def can_reserve(self, quantity: int) -> bool:
        """Check if the requested quantity can be reserved."""
        return self.quantity_available >= quantity

    def reserve(self, quantity: int) -> None:
        """
        Reserve stock for an order.

        Moves quantity from available to reserved.
        Raises ValueError if insufficient stock.
        """
        if not self.can_reserve(quantity):
            raise ValueError(
                f"Cannot reserve {quantity} units. "
                f"Only {self.quantity_available} available."
            )
        self.quantity_available -= quantity
        self.quantity_reserved += quantity
        self.updated_at = datetime.utcnow()

    def release(self, quantity: int) -> None:
        """
        Release reserved stock (compensation action).

        Moves quantity from reserved back to available.
        Used when an order is cancelled after stock was reserved.
        """
        release_qty = min(quantity, self.quantity_reserved)
        self.quantity_reserved -= release_qty
        self.quantity_available += release_qty
        self.updated_at = datetime.utcnow()

    def add_stock(self, quantity: int) -> None:
        """Add new stock to available quantity."""
        if quantity <= 0:
            raise ValueError("Quantity must be positive")
        self.quantity_available += quantity
        self.updated_at = datetime.utcnow()

    def to_dict(self) -> dict:
        """Convert to dictionary for serialization."""
        return {
            "id": str(self.id),
            "product_id": str(self.product_id),
            "product_name": self.product_name,
            "quantity_available": self.quantity_available,
            "quantity_reserved": self.quantity_reserved,
            "total_stock": self.total_stock,
            "updated_at": self.updated_at.isoformat(),
        }
