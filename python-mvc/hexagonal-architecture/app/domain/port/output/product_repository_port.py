"""
Output Port: Product Repository Port.

This port defines what the domain NEEDS from the persistence layer.
It is part of the domain and must not depend on any infrastructure detail.

In Hexagonal Architecture, output ports are implemented by output adapters
(e.g., database repositories, external API clients, file system adapters).
The domain depends on this abstraction, not on concrete implementations.
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Optional

from app.domain.model.product import Product


class ProductRepositoryPort(ABC):
    """
    Output port for product persistence operations.

    This interface is defined in the domain layer and implemented
    by output adapters (InMemoryRepository, PostgresRepository, etc.).
    """

    @abstractmethod
    async def save(self, product: Product) -> Product:
        """Persist a new product."""
        ...

    @abstractmethod
    async def find_by_id(self, product_id: str) -> Optional[Product]:
        """Find a product by its unique identifier."""
        ...

    @abstractmethod
    async def find_all(self, page: int, size: int) -> tuple[list[Product], int]:
        """
        Find all products with pagination.

        Returns a tuple of (products_list, total_count).
        """
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
        min_price: Optional[float] = None,
        max_price: Optional[float] = None,
    ) -> list[Product]:
        """Search products by various criteria."""
        ...

    @abstractmethod
    async def update(self, product: Product) -> Product:
        """Update an existing product."""
        ...

    @abstractmethod
    async def delete(self, product_id: str) -> None:
        """Delete a product by its identifier."""
        ...
