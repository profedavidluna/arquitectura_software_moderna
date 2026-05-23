"""
@layer Use Cases
@description Stock Management use cases - increase and decrease product stock.

These two use cases are grouped in one file because they are closely related,
but each is still a separate class following SRP.
"""

from app.entities.product import Product
from app.usecases.dto import ProductOutput
from app.usecases.errors import ProductNotFoundError
from app.usecases.interfaces.product_gateway import ProductGateway


class DecreaseStockUseCase:
    """
    Use case: Decrease product stock.

    Business rule: stock cannot go negative (enforced by entity).
    """

    def __init__(self, gateway: ProductGateway):
        self._gateway = gateway

    async def execute(self, product_id: str, quantity: int) -> ProductOutput:
        """Execute the decrease stock use case."""
        product = await self._gateway.find_by_id(product_id)
        if not product:
            raise ProductNotFoundError(product_id)

        # Entity enforces the business rule (stock >= 0)
        updated = product.decrease_stock(quantity)
        saved = await self._gateway.update(updated)
        return self._to_output(saved)

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


class IncreaseStockUseCase:
    """
    Use case: Increase product stock.
    """

    def __init__(self, gateway: ProductGateway):
        self._gateway = gateway

    async def execute(self, product_id: str, quantity: int) -> ProductOutput:
        """Execute the increase stock use case."""
        product = await self._gateway.find_by_id(product_id)
        if not product:
            raise ProductNotFoundError(product_id)

        updated = product.increase_stock(quantity)
        saved = await self._gateway.update(updated)
        return self._to_output(saved)

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
