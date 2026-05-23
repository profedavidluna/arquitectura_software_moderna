"""
@layer Use Cases
@description Delete Product use case - soft-deletes a product.
"""

from app.entities.product import Product
from app.usecases.errors import ProductNotFoundError
from app.usecases.interfaces.product_gateway import ProductGateway


class DeleteProductUseCase:
    """
    Use case: Soft-delete a product (set active=False).

    Steps:
    1. Find the product
    2. Deactivate via entity method
    3. Persist the change
    """

    def __init__(self, gateway: ProductGateway):
        self._gateway = gateway

    async def execute(self, product_id: str) -> None:
        """Execute the delete product use case."""
        product = await self._gateway.find_by_id(product_id)
        if not product:
            raise ProductNotFoundError(product_id)

        # Use entity's deactivate method (soft delete)
        deactivated = product.deactivate()
        await self._gateway.update(deactivated)
