"""
@layer Use Cases
@description Update Product use case - updates an existing product.
"""

from app.entities.product import Product
from app.usecases.dto import ProductOutput, UpdateProductInput
from app.usecases.errors import DuplicateSkuError, ProductNotFoundError
from app.usecases.interfaces.product_gateway import ProductGateway


class UpdateProductUseCase:
    """
    Use case: Update an existing product.

    Steps:
    1. Find the product
    2. Check SKU uniqueness if changing
    3. Apply updates via entity method (validates business rules)
    4. Persist changes
    """

    def __init__(self, gateway: ProductGateway):
        self._gateway = gateway

    async def execute(self, input_data: UpdateProductInput) -> ProductOutput:
        """Execute the update product use case."""
        # Find existing product
        product = await self._gateway.find_by_id(input_data.product_id)
        if not product:
            raise ProductNotFoundError(input_data.product_id)

        # Check SKU uniqueness if changing
        if input_data.sku and input_data.sku != product.sku:
            existing = await self._gateway.find_by_sku(input_data.sku)
            if existing:
                raise DuplicateSkuError(input_data.sku)

        # Update via entity (validates business rules)
        updated = product.update(
            name=input_data.name,
            description=input_data.description,
            price=input_data.price,
            category=input_data.category,
            stock_quantity=input_data.stock_quantity,
            sku=input_data.sku,
        )

        # Persist
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
