"""
Domain Service: Product Service.

This service implements the input port (ProductServicePort) and orchestrates
domain logic by coordinating between the domain model and the output port
(ProductRepositoryPort).

It contains application-level business rules such as:
- Validating uniqueness constraints (SKU)
- Coordinating persistence operations
- Enforcing pagination limits

This service depends ONLY on domain abstractions (ports), never on
concrete infrastructure implementations.
"""
from __future__ import annotations

import math
from dataclasses import replace
from datetime import datetime

from app.domain.model.product import (
    DuplicateSkuError,
    Product,
    ProductNotFoundError,
)
from app.domain.port.input.product_service_port import (
    CreateProductCommand,
    PaginatedResult,
    ProductServicePort,
    SearchCriteria,
    UpdateProductCommand,
)
from app.domain.port.output.product_repository_port import ProductRepositoryPort


class ProductService(ProductServicePort):
    """
    Implementation of the ProductServicePort.

    Orchestrates domain operations using the repository port for persistence.
    """

    MAX_PAGE_SIZE = 100

    def __init__(self, repository: ProductRepositoryPort) -> None:
        self._repository = repository

    async def create_product(self, command: CreateProductCommand) -> Product:
        """Create a new product after validating business rules."""
        # Validate business rules
        Product.validate_price(command.price)
        Product.validate_stock(command.stock_quantity)

        # Check SKU uniqueness
        existing = await self._repository.find_by_sku(command.sku)
        if existing is not None:
            raise DuplicateSkuError(f"Product with SKU '{command.sku}' already exists")

        # Create domain entity
        now = datetime.utcnow()
        product = Product(
            id=Product.generate_id(),
            name=command.name,
            description=command.description,
            price=command.price,
            category=command.category,
            stock_quantity=command.stock_quantity,
            sku=command.sku,
            active=True,
            created_at=now,
            updated_at=now,
        )

        return await self._repository.save(product)

    async def get_product_by_id(self, product_id: str) -> Product:
        """Retrieve a product by ID or raise ProductNotFoundError."""
        product = await self._repository.find_by_id(product_id)
        if product is None:
            raise ProductNotFoundError(f"Product with id '{product_id}' not found")
        return product

    async def list_products(self, page: int = 1, size: int = 10) -> PaginatedResult:
        """List products with pagination, enforcing max page size."""
        page = max(1, page)
        size = max(1, min(size, self.MAX_PAGE_SIZE))

        items, total = await self._repository.find_all(page, size)
        total_pages = math.ceil(total / size) if total > 0 else 0

        return PaginatedResult(
            items=items,
            total=total,
            page=page,
            size=size,
            total_pages=total_pages,
        )

    async def search_products(self, criteria: SearchCriteria) -> list[Product]:
        """Search products based on criteria."""
        return await self._repository.search(
            query=criteria.query,
            category=criteria.category,
            min_price=criteria.min_price,
            max_price=criteria.max_price,
        )

    async def update_product(
        self, product_id: str, command: UpdateProductCommand
    ) -> Product:
        """Update an existing product with the provided fields."""
        product = await self._repository.find_by_id(product_id)
        if product is None:
            raise ProductNotFoundError(f"Product with id '{product_id}' not found")

        # Validate new values if provided
        if command.price is not None:
            Product.validate_price(command.price)
        if command.stock_quantity is not None:
            Product.validate_stock(command.stock_quantity)

        # Check SKU uniqueness if changing SKU
        if command.sku is not None and command.sku != product.sku:
            existing = await self._repository.find_by_sku(command.sku)
            if existing is not None:
                raise DuplicateSkuError(
                    f"Product with SKU '{command.sku}' already exists"
                )

        # Build updated product using only provided fields
        updates: dict = {}
        if command.name is not None:
            updates["name"] = command.name
        if command.description is not None:
            updates["description"] = command.description
        if command.price is not None:
            updates["price"] = command.price
        if command.category is not None:
            updates["category"] = command.category
        if command.stock_quantity is not None:
            updates["stock_quantity"] = command.stock_quantity
        if command.sku is not None:
            updates["sku"] = command.sku
        if command.active is not None:
            updates["active"] = command.active

        updates["updated_at"] = datetime.utcnow()
        updated_product = replace(product, **updates)

        return await self._repository.update(updated_product)

    async def delete_product(self, product_id: str) -> None:
        """Delete a product by ID."""
        product = await self._repository.find_by_id(product_id)
        if product is None:
            raise ProductNotFoundError(f"Product with id '{product_id}' not found")
        await self._repository.delete(product_id)

    async def decrease_stock(self, product_id: str, quantity: int) -> Product:
        """Decrease stock for a product."""
        product = await self._repository.find_by_id(product_id)
        if product is None:
            raise ProductNotFoundError(f"Product with id '{product_id}' not found")

        updated_product = product.decrease_stock(quantity)
        return await self._repository.update(updated_product)

    async def increase_stock(self, product_id: str, quantity: int) -> Product:
        """Increase stock for a product."""
        product = await self._repository.find_by_id(product_id)
        if product is None:
            raise ProductNotFoundError(f"Product with id '{product_id}' not found")

        updated_product = product.increase_stock(quantity)
        return await self._repository.update(updated_product)
