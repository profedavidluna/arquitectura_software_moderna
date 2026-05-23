"""
Output Adapter: In-Memory Product Repository.

This is a driven/secondary adapter that implements the ProductRepositoryPort
using an in-memory dictionary. It is useful for:
- Development without a database
- Unit testing the domain service
- Rapid prototyping

In Hexagonal Architecture, output adapters implement the output ports
defined in the domain layer, providing concrete infrastructure behavior.
"""
from __future__ import annotations

from typing import Optional

from app.domain.model.product import Product
from app.domain.port.output.product_repository_port import ProductRepositoryPort


class InMemoryProductRepository(ProductRepositoryPort):
    """
    In-memory implementation of the ProductRepositoryPort.

    Stores products in a Python dictionary for fast lookups.
    Data is lost when the application restarts.
    """

    def __init__(self) -> None:
        self._storage: dict[str, Product] = {}

    async def save(self, product: Product) -> Product:
        """Persist a new product in memory."""
        self._storage[product.id] = product
        return product

    async def find_by_id(self, product_id: str) -> Optional[Product]:
        """Find a product by its unique identifier."""
        return self._storage.get(product_id)

    async def find_all(self, page: int, size: int) -> tuple[list[Product], int]:
        """
        Find all products with pagination.

        Returns a tuple of (products_list, total_count).
        """
        all_products = list(self._storage.values())
        total = len(all_products)

        start = (page - 1) * size
        end = start + size
        items = all_products[start:end]

        return items, total

    async def find_by_sku(self, sku: str) -> Optional[Product]:
        """Find a product by its SKU."""
        for product in self._storage.values():
            if product.sku == sku:
                return product
        return None

    async def search(
        self,
        query: Optional[str] = None,
        category: Optional[str] = None,
        min_price: Optional[float] = None,
        max_price: Optional[float] = None,
    ) -> list[Product]:
        """Search products by various criteria."""
        results = list(self._storage.values())

        if query:
            query_lower = query.lower()
            results = [
                p
                for p in results
                if query_lower in p.name.lower()
                or query_lower in p.description.lower()
            ]

        if category:
            category_lower = category.lower()
            results = [
                p for p in results if p.category.lower() == category_lower
            ]

        if min_price is not None:
            results = [p for p in results if p.price >= min_price]

        if max_price is not None:
            results = [p for p in results if p.price <= max_price]

        return results

    async def update(self, product: Product) -> Product:
        """Update an existing product in memory."""
        self._storage[product.id] = product
        return product

    async def delete(self, product_id: str) -> None:
        """Delete a product by its identifier."""
        self._storage.pop(product_id, None)
