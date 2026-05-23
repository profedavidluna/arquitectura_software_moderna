"""
Inventory Events
==================
Factory methods for creating inventory-related domain events.

Events Published:
- stock.reserved: Stock successfully reserved (saga response - happy path)
- stock.insufficient: Cannot reserve stock (saga response - compensation trigger)
- stock.released: Stock released after order cancellation
"""

from datetime import datetime
from uuid import UUID


class InventoryEvents:
    """Factory class for creating inventory-related domain events."""

    @staticmethod
    def stock_reserved(order_id: UUID, items: list[dict]) -> dict:
        """
        Create a 'stock.reserved' event payload.

        Published when stock is successfully reserved for all order items.
        The Order Service consumes this to confirm the order.
        """
        return {
            "event_type": "stock.reserved",
            "timestamp": datetime.utcnow().isoformat(),
            "data": {
                "order_id": str(order_id),
                "items": [
                    {
                        "product_id": item["product_id"],
                        "quantity": item["quantity"],
                    }
                    for item in items
                ],
            },
        }

    @staticmethod
    def stock_insufficient(order_id: UUID, reason: str) -> dict:
        """
        Create a 'stock.insufficient' event payload.

        Published when stock cannot be reserved for the order.
        The Order Service consumes this to cancel the order (compensation).
        """
        return {
            "event_type": "stock.insufficient",
            "timestamp": datetime.utcnow().isoformat(),
            "data": {
                "order_id": str(order_id),
                "reason": reason,
            },
        }

    @staticmethod
    def stock_released(order_id: UUID, items: list[dict]) -> dict:
        """
        Create a 'stock.released' event payload.

        Published after releasing reserved stock due to order cancellation.
        Informational event for audit/monitoring purposes.
        """
        return {
            "event_type": "stock.released",
            "timestamp": datetime.utcnow().isoformat(),
            "data": {
                "order_id": str(order_id),
                "items": [
                    {
                        "product_id": item["product_id"],
                        "quantity": item["quantity"],
                    }
                    for item in items
                ],
            },
        }
