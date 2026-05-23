"""
@layer Use Cases
@description Get Product use case - retrieves a single product by ID.
"""

from app.entities.product import Product
from app.usecases.dto import ProductOutput
from app.usecases.errors import ProductNotFoundError
from app.usecases.interfaces.product_gateway import ProductGateway


class GetProductUseCase:
    """
    Use case: Get a product by its ID.

    Simple retrieval use case. Raises ProductNotFoundError if not found.
    """

    def __init__(self, gateway: ProductGateway):
        self._gateway = gateway

    async def execute(self, product_id: str) -> ProductOutput:
        """Execute the get product use case."""
        product = await self._gateway.find_by_id(product_id)
        if not product:
            raise ProductNotFoundError(product_id)
        return self._to_output(product)

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
