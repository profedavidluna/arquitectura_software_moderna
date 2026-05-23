"""
@layer Use Cases - Gateway Interface
@description Defines the data access interface needed by use cases.

In Clean Architecture, the gateway interface is defined in the USE CASES layer
(not in the frameworks layer). This is the Dependency Inversion Principle:
- The use case defines WHAT it needs (this interface)
- The frameworks layer provides HOW (concrete implementation)

This ensures the Dependency Rule is maintained:
outer layers depend on inner layers, never the reverse.
"""

from abc import ABC, abstractmethod
from decimal import Decimal
from typing import Optional

from app.entities.product import Product


class ProductGateway(ABC):
    """
    Gateway interface for product data access.

    Defined in the use cases layer because use cases need to specify
    what data operations they require. The concrete implementation
    lives in the frameworks layer (PostgreSQL, in-memory, etc.)
    """

    @abstractmethod
    async def save(self, product: Product) -> Product:
        """Persist a new product."""
        ...

    @abstractmethod
    async def find_by_id(self, product_id: str) -> Optional[Product]:
        """Find a product by its unique ID."""
        ...

    @abstractmethod
    async def find_all(self, page: int, size: int) -> dict:
        """Find all active products with pagination."""
        ...

    @abstractmethod
    async def find_by_sku(self, sku: str) -> Optional[Product]:
        """Find a product by its SKU."""
        ...

    @abstractmethod
    async def search(
        self,
        query: Optional[str] = None,
        category: Optional[str] = None,
        min_price: Optional[Decimal] = None,
        max_price: Optional[Decimal] = None,
    ) -> list[Product]:
        """Search products by various criteria."""
        ...

    @abstractmethod
    async def update(self, product: Product) -> Product:
        """Update an existing product."""
        ...

    @abstractmethod
    async def delete(self, product_id: str) -> None:
        """Delete a product."""
        ...
