"""
Order Domain Models
====================
Core business entities for the Order Service.

Design Pattern: Domain Model with State Machine
- Order has a well-defined state machine (OrderStatus)
- State transitions are enforced by the domain model
- Invalid transitions raise exceptions

Order States (Saga States):
    PENDING → CONFIRMED (stock reserved successfully)
    PENDING → CANCELLED (stock insufficient - compensation)
    CONFIRMED → CANCELLED (manual cancellation - triggers stock release)
"""

from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal
from enum import Enum
from uuid import UUID


class OrderStatus(str, Enum):
    """
    Order status representing saga states.

    PENDING: Order created, waiting for inventory confirmation
    CONFIRMED: Stock reserved, order is confirmed
    CANCELLED: Order cancelled (insufficient stock or manual cancellation)
    """

    PENDING = "PENDING"
    CONFIRMED = "CONFIRMED"
    CANCELLED = "CANCELLED"


@dataclass
class OrderItem:
    """
    Represents a line item in an order.

    Each item references a product and specifies quantity and pricing.
    The subtotal is pre-calculated for efficiency.
    """

    id: UUID
    order_id: UUID
    product_id: UUID
    product_name: str
    quantity: int
    unit_price: Decimal
    subtotal: Decimal


@dataclass
class Order:
    """
    Aggregate root for the Order domain.

    The Order entity manages its items and enforces business rules
    around state transitions. It acts as the saga state holder.

    Attributes:
        id: Unique order identifier
        user_id: The customer who placed the order
        status: Current saga state
        total_amount: Sum of all item subtotals
        items: Line items in the order
        created_at: When the order was placed
        updated_at: Last state change timestamp
    """

    id: UUID
    user_id: UUID
    status: OrderStatus = OrderStatus.PENDING
    total_amount: Decimal = Decimal("0.00")
    items: list[OrderItem] = field(default_factory=list)
    created_at: datetime = field(default_factory=datetime.utcnow)
    updated_at: datetime = field(default_factory=datetime.utcnow)

    def confirm(self) -> None:
        """
        Confirm the order (stock was reserved successfully).

        This is called when the Inventory Service responds with
        'stock.reserved', completing the happy path of the saga.
        """
        if self.status != OrderStatus.PENDING:
            raise ValueError(f"Cannot confirm order in status {self.status}")
        self.status = OrderStatus.CONFIRMED
        self.updated_at = datetime.utcnow()

    def cancel(self) -> None:
        """
        Cancel the order (compensation action).

        Called when:
        - Inventory Service responds with 'stock.insufficient'
        - User manually cancels a confirmed order
        """
        if self.status == OrderStatus.CANCELLED:
            raise ValueError("Order is already cancelled")
        self.status = OrderStatus.CANCELLED
        self.updated_at = datetime.utcnow()

    def calculate_total(self) -> None:
        """Recalculate total amount from items."""
        self.total_amount = sum(item.subtotal for item in self.items)

    def to_dict(self) -> dict:
        """Convert to dictionary for serialization."""
        return {
            "id": str(self.id),
            "user_id": str(self.user_id),
            "status": self.status.value,
            "total_amount": float(self.total_amount),
            "items": [
                {
                    "id": str(item.id),
                    "product_id": str(item.product_id),
                    "product_name": item.product_name,
                    "quantity": item.quantity,
                    "unit_price": float(item.unit_price),
                    "subtotal": float(item.subtotal),
                }
                for item in self.items
            ],
            "created_at": self.created_at.isoformat(),
            "updated_at": self.updated_at.isoformat(),
        }
