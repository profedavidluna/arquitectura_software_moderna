"""
Product Service Interface (Protocol)
======================================
Defines the contract for the Product Service using Python's Protocol.

SOLID Principle: Interface Segregation (ISP)
- The interface is focused and cohesive
- Clients depend only on methods they use

SOLID Principle: Dependency Inversion (DIP)
- High-level modules depend on this abstraction
- Low-level modules implement this abstraction
- Both are decoupled through the Protocol

Design Pattern: Service Layer
- Defines operations available to the outside world
- Acts as a boundary between domain and infrastructure
"""

from typing import Protocol
from uuid import UUID

from app.domain.model import Product


class IProductService(Protocol):
    """
    Protocol defining the Product Service contract.

    Any class implementing this protocol must provide these methods.
    Python's structural typing means no explicit inheritance is needed -
    if a class has these methods with matching signatures, it satisfies
    the protocol (duck typing with type safety).
    """

    async def create_product(
        self,
        name: str,
        description: str,
        price: float,
        category: str,
        sku: str,
    ) -> Product:
        """Create a new product and publish creation event."""
        ...

    async def get_by_id(self, product_id: UUID) -> Product | None:
        """Retrieve a product by its unique identifier."""
        ...

    async def get_all(self) -> list[Product]:
        """Retrieve all active products."""
        ...

    async def update_product(
        self,
        product_id: UUID,
        name: str | None = None,
        description: str | None = None,
        price: float | None = None,
        category: str | None = None,
    ) -> Product | None:
        """Update an existing product's attributes."""
        ...

    async def delete_product(self, product_id: UUID) -> bool:
        """Soft-delete a product (deactivate)."""
        ...
