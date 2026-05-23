"""
Product Events
===============
Factory methods for creating domain events.

Design Pattern: Factory
- Centralizes event creation logic
- Ensures consistent event structure
- Makes it easy to evolve event schemas

SOA Principle: Standardized Service Contract
- Events follow a consistent schema
- All events include metadata (timestamp, event type)
- Consumers can rely on the event structure
"""

from datetime import datetime

from app.domain.model import Product


class ProductEvents:
    """Factory class for creating product-related domain events."""

    @staticmethod
    def product_created(product: Product) -> dict:
        """
        Create a 'product.created' event payload.

        This event is consumed by the Inventory Service to automatically
        create an inventory entry for the new product.
        """
        return {
            "event_type": "product.created",
            "timestamp": datetime.utcnow().isoformat(),
            "data": {
                "product_id": str(product.id),
                "name": product.name,
                "description": product.description,
                "price": float(product.price),
                "category": product.category,
                "sku": product.sku,
            },
        }

    @staticmethod
    def product_updated(product: Product) -> dict:
        """Create a 'product.updated' event payload."""
        return {
            "event_type": "product.updated",
            "timestamp": datetime.utcnow().isoformat(),
            "data": {
                "product_id": str(product.id),
                "name": product.name,
                "price": float(product.price),
                "category": product.category,
            },
        }
