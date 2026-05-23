"""
@layer Use Cases
@description Create Product use case - handles the creation of a new product.

Each use case class in Clean Architecture:
- Has a single responsibility (SRP)
- Has a single public method: execute()
- Receives the gateway via constructor injection
- Orchestrates entities and gateway to fulfill the use case
"""

from app.entities.product import Product
from app.usecases.dto import CreateProductInput, ProductOutput
from app.usecases.errors import DuplicateSkuError
from app.usecases.interfaces.product_gateway import ProductGateway


class CreateProductUseCase:
    """
    Use case: Create a new product in the catalog.

    Steps:
    1. Check SKU uniqueness via gateway
    2. Create entity (validates business rules)
    3. Persist via gateway
    4. Return output DTO
    """

    def __init__(self, gateway: ProductGateway):
        self._gateway = gateway

    async def execute(self, input_data: CreateProductInput) -> ProductOutput:
        """Execute the create product use case."""
        # Check SKU uniqueness
        existing = await self._gateway.find_by_sku(input_data.sku)
        if existing:
            raise DuplicateSkuError(input_data.sku)

        # Create entity (business rules validated in factory method)
        product = Product.create(
            name=input_data.name,
            description=input_data.description,
            price=input_data.price,
            category=input_data.category,
            stock_quantity=input_data.stock_quantity,
            sku=input_data.sku,
        )

        # Persist
        saved = await self._gateway.save(product)

        # Return output DTO
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
