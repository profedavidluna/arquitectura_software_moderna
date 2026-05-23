"""
Order Service Interface (Protocol)
====================================
Defines the contract for the Order Service.

The interface includes both API-facing operations and
saga callback methods that are invoked by event consumers.
"""

from typing import Protocol
from uuid import UUID

from app.domain.model import Order, OrderItem


class IOrderService(Protocol):
    """
    Protocol defining the Order Service contract.

    Includes:
    - CRUD operations exposed via REST API
    - Saga callback methods invoked by Kafka consumers
    """

    async def create_order(
        self,
        user_id: UUID,
        items: list[dict],
    ) -> Order:
        """Create a new order and initiate the order saga."""
        ...

    async def get_by_id(self, order_id: UUID) -> Order | None:
        """Retrieve an order by its ID."""
        ...

    async def get_by_user(self, user_id: UUID) -> list[Order]:
        """Retrieve all orders for a specific user."""
        ...

    async def confirm_order(self, order_id: UUID) -> None:
        """
        Saga callback: Confirm order after stock reservation.
        Called when 'stock.reserved' event is received.
        """
        ...

    async def cancel_order(self, order_id: UUID, reason: str) -> None:
        """
        Saga callback: Cancel order due to insufficient stock.
        Called when 'stock.insufficient' event is received.
        Also used for manual cancellation.
        """
        ...
