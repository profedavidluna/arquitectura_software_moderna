"""
@layer Use Cases
@description List Products use case - retrieves paginated product list.
"""

from app.entities.product import Product
from app.usecases.dto import ProductListOutput, ProductOutput
from app.usecases.interfaces.product_gateway import ProductGateway


class ListProductsUseCase:
    """
    Use case: List all active products with pagination.

    Enforces the max page size business rule.
    """

    MAX_PAGE_SIZE = 100

    def __init__(self, gateway: ProductGateway):
        self._gateway = gateway

    async def execute(self, page: int, size: int) -> ProductListOutput:
        """Execute the list products use case."""
        effective_size = min(size, self.MAX_PAGE_SIZE)
        result = await self._gateway.find_all(page, effective_size)

        return ProductListOutput(
            products=[self._to_output(p) for p in result["products"]],
            total=result["total"],
            page=page,
            size=effective_size,
        )

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
