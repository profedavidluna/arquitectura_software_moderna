"""
@layer Frameworks (Outermost Layer) - In-Memory Gateway
@description Implements the ProductGateway interface using an in-memory dictionary.

This implementation is useful for:
- Running without a database
- Unit testing use cases in isolation
- Demonstrating the Dependency Rule (use cases don't know about this)

The use cases layer defines the interface (ProductGateway ABC).
This class in the frameworks layer provides the concrete implementation.
"""

from decimal import Decimal
from typing import Optional

from app.entities.product import Product
from app.usecases.interfaces.product_gateway import ProductGateway


class InMemoryProductGateway(ProductGateway):
    """
    In-memory implementation of ProductGateway.

    Stores products in a Python dictionary.
    Implements the interface defined in the use cases layer.
    """

    def __init__(self):
        self._products: dict[str, Product] = {}

    async def save(self, product: Product) -> Product:
        self._products[product.id] = product
        return product

    async def find_by_id(self, product_id: str) -> Optional[Product]:
        return self._products.get(product_id)

    async def find_all(self, page: int, size: int) -> dict:
        active_products = [p for p in self._products.values() if p.active]
        total = len(active_products)
        start = (page - 1) * size
        end = start + size
        return {"products": active_products[start:end], "total": total}

    async def find_by_sku(self, sku: str) -> Optional[Product]:
        for product in self._products.values():
            if product.sku == sku:
                return product
        return None

    async def search(
        self,
        query: Optional[str] = None,
        category: Optional[str] = None,
        min_price: Optional[Decimal] = None,
        max_price: Optional[Decimal] = None,
    ) -> list[Product]:
        results = [p for p in self._products.values() if p.active]

        if query:
            query_lower = query.lower()
            results = [
                p for p in results
                if query_lower in p.name.lower() or query_lower in p.description.lower()
            ]

        if category:
            results = [p for p in results if p.category.lower() == category.lower()]

        if min_price is not None:
            results = [p for p in results if p.price >= min_price]

        if max_price is not None:
            results = [p for p in results if p.price <= max_price]

        return results

    async def update(self, product: Product) -> Product:
        self._products[product.id] = product
        return product

    async def delete(self, product_id: str) -> None:
        self._products.pop(product_id, None)
