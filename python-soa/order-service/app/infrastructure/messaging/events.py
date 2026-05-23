"""
Order Events
==============
Factory methods for creating order-related domain events.

Events Published:
- order.created: Initiates the saga (consumed by Inventory Service)
- order.confirmed: Saga completed successfully
- order.cancelled: Compensation event (triggers stock release)
"""

from datetime import datetime

from app.domain.model import Order


class OrderEvents:
    """Factory class for creating order-related domain events."""

    @staticmethod
    def order_created(order: Order) -> dict:
        """
        Create an 'order.created' event payload.

        This event initiates the order saga. The Inventory Service
        consumes it and attempts to reserve stock for all items.

        Contains all item details needed for stock reservation.
        """
        return {
            "event_type": "order.created",
            "timestamp": datetime.utcnow().isoformat(),
            "data": {
                "order_id": str(order.id),
                "user_id": str(order.user_id),
                "items": [
                    {
                        "product_id": str(item.product_id),
                        "product_name": item.product_name,
                        "quantity": item.quantity,
                        "unit_price": float(item.unit_price),
                    }
                    for item in order.items
                ],
                "total_amount": float(order.total_amount),
            },
        }

    @staticmethod
    def order_confirmed(order: Order) -> dict:
        """
        Create an 'order.confirmed' event payload.

        Published when the saga completes successfully.
        Can be consumed by notification services, analytics, etc.
        """
        return {
            "event_type": "order.confirmed",
            "timestamp": datetime.utcnow().isoformat(),
            "data": {
                "order_id": str(order.id),
                "user_id": str(order.user_id),
                "total_amount": float(order.total_amount),
                "status": order.status.value,
            },
        }

    @staticmethod
    def order_cancelled(order: Order, reason: str = "") -> dict:
        """
        Create an 'order.cancelled' event payload.

        Published when a confirmed order is cancelled.
        The Inventory Service consumes this to release reserved stock.
        """
        return {
            "event_type": "order.cancelled",
            "timestamp": datetime.utcnow().isoformat(),
            "data": {
                "order_id": str(order.id),
                "user_id": str(order.user_id),
                "reason": reason,
                "items": [
                    {
                        "product_id": str(item.product_id),
                        "quantity": item.quantity,
                    }
                    for item in order.items
                ],
            },
        }
