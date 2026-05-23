"""
@layer Use Cases
@description Search Products use case - searches products by criteria.
"""

from decimal import Decimal
from typing import Optional

from app.entities.product import Product
from app.usecases.dto import ProductOutput
from app.usecases.interfaces.product_gateway import ProductGateway


class SearchProductsUseCase:
    """
    Use case: Search products by query, category, and price range.
    """

    def __init__(self, gateway: ProductGateway):
        self._gateway = gateway

    async def execute(
        self,
        query: Optional[str] = None,
        category: Optional[str] = None,
        min_price: Optional[Decimal] = None,
        max_price: Optional[Decimal] = None,
    ) -> list[ProductOutput]:
        """Execute the search products use case."""
        products = await self._gateway.search(query, category, min_price, max_price)
        return [self._to_output(p) for p in products]

    @staticmethod
    def _to_output(product: Product) -> ProductOutput:
        return ProductOutput(
            id=product.id,
            name=product.name,
            description=product.description,
            price=product.price,
            category=product.category,
            stock_quantity=product.stock_quantity,
            sku=product.sku,
            active=product.active,
            created_at=product.created_at,
            updated_at=product.updated_at,
        )
