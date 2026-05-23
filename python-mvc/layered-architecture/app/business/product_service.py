"""
Business Layer: Product Service.

Contains all business logic for product operations. This is the middle layer
in the Layered Architecture pattern: Presentation → Business → Data.

KEY DIFFERENCE FROM HEXAGONAL ARCHITECTURE:
- Hexagonal: The service implements an abstract input port and depends on an
  abstract output port. Both are interfaces defined in the domain layer.
  The service never knows about concrete implementations.
- Layered: The service is a concrete class that receives a repository via
  constructor injection using duck typing (no ABC/interface). It directly
  knows about the Data Layer's models and could even import concrete repository
  classes if needed.

Benefits of Layered:
- Simpler to understand and implement
- Less boilerplate (no port interfaces)
- Faster development for small/medium projects

Drawbacks of Layered:
- Tighter coupling between layers
- Harder to swap implementations without changing service code
- Business rules mixed with orchestration logic
- Testing requires the actual repository class (or duck-typed substitute)
"""
from __future__ import annotations

import math
from datetime import datetime
from typing import Optional
from uuid import uuid4

from app.business.errors import (
    ConflictError,
    InsufficientStockError,
    NotFoundError,
    ValidationError,
)
from app.data.models import Product


class ProductService:
    """
    Product business service.

    Orchestrates all product operations with business rule validation.
    Constructor takes a repository object (duck typing — no interface required).
    """

    MAX_PAGE_SIZE = 100

    def __init__(self, repository) -> None:
        """
        Initialize with a repository instance.

        The repository is accepted via duck typing — any object with the
        expected methods (save, find_by_id, find_all, etc.) will work.
        No abstract base class or interface is enforced.
        """
        self._repository = repository

    async def create_product(
        self,
        name: str,
        description: str,
        price: float,
        category: str,
        stock_quantity: int,
        sku: str,
    ) -> Product:
        """
        Create a new product after validating business rules.

        Validates:
        - Price must be greater than 0
        - Stock quantity must be >= 0
        - SKU must be unique
        """
        # Validate business rules
        if price <= 0:
            raise ValidationError(f"Price must be greater than 0, got {price}")
        if stock_quantity < 0:
            raise ValidationError(
                f"Stock quantity cannot be negative, got {stock_quantity}"
            )

        # Check SKU uniqueness
        existing = await self._repository.find_by_sku(sku)
        if existing is not None:
            raise ConflictError(f"Product with SKU '{sku}' already exists")

        # Create product model
        now = datetime.utcnow()
        product = Product(
            id=str(uuid4()),
            name=name,
            description=description,
            price=price,
            category=category,
            stock_quantity=stock_quantity,
            sku=sku,
            active=True,
            created_at=now,
            updated_at=now,
        )

        return await self._repository.save(product)

    async def get_product_by_id(self, product_id: str) -> Product:
        """Retrieve a product by ID or raise NotFoundError."""
        product = await self._repository.find_by_id(product_id)
        if product is None:
            raise NotFoundError(f"Product with id '{product_id}' not found")
        return product

    async def list_products(
        self, page: int = 1, size: int = 10
    ) -> dict:
        """
        List products with pagination, enforcing max page size of 100.

        Returns a dict with items, total, page, size, and total_pages.
        """
        page = max(1, page)
        size = max(1, min(size, self.MAX_PAGE_SIZE))

        items, total = await self._repository.find_all(page, size)
        total_pages = math.ceil(total / size) if total > 0 else 0

        return {
            "items": items,
            "total": total,
            "page": page,
            "size": size,
            "total_pages": total_pages,
        }

    async def search_products(
        self,
        query: Optional[str] = None,
        category: Optional[str] = None,
        min_price: Optional[float] = None,
        max_price: Optional[float] = None,
    ) -> list[Product]:
        """Search products based on criteria."""
        return await self._repository.search(
            query=query,
            category=category,
            min_price=min_price,
            max_price=max_price,
        )

    async def update_product(
        self,
        product_id: str,
        name: Optional[str] = None,
        description: Optional[str] = None,
        price: Optional[float] = None,
        category: Optional[str] = None,
        stock_quantity: Optional[int] = None,
        sku: Optional[str] = None,
        active: Optional[bool] = None,
    ) -> Product:
        """Update an existing product with the provided fields."""
        product = await self._repository.find_by_id(product_id)
        if product is None:
            raise NotFoundError(f"Product with id '{product_id}' not found")

        # Validate new values if provided
        if price is not None and price <= 0:
            raise ValidationError(f"Price must be greater than 0, got {price}")
        if stock_quantity is not None and stock_quantity < 0:
            raise ValidationError(
                f"Stock quantity cannot be negative, got {stock_quantity}"
            )

        # Check SKU uniqueness if changing SKU
        if sku is not None and sku != product.sku:
            existing = await self._repository.find_by_sku(sku)
            if existing is not None:
                raise ConflictError(f"Product with SKU '{sku}' already exists")

        # Mutate the product directly (layered architecture uses mutable models)
        if name is not None:
            product.name = name
        if description is not None:
            product.description = description
        if price is not None:
            product.price = price
        if category is not None:
            product.category = category
        if stock_quantity is not None:
            product.stock_quantity = stock_quantity
        if sku is not None:
            product.sku = sku
        if active is not None:
            product.active = active

        product.updated_at = datetime.utcnow()

        return await self._repository.update(product)

    async def delete_product(self, product_id: str) -> None:
        """Delete a product by ID."""
        product = await self._repository.find_by_id(product_id)
        if product is None:
            raise NotFoundError(f"Product with id '{product_id}' not found")
        await self._repository.delete(product_id)

    async def decrease_stock(self, product_id: str, quantity: int) -> Product:
        """
        Decrease stock for a product.

        Validates that quantity is positive and sufficient stock exists.
        """
        product = await self._repository.find_by_id(product_id)
        if product is None:
            raise NotFoundError(f"Product with id '{product_id}' not found")

        if quantity <= 0:
            raise ValidationError("Decrease quantity must be positive")
        if quantity > product.stock_quantity:
            raise InsufficientStockError(
                f"Cannot decrease stock by {quantity}. "
                f"Current stock: {product.stock_quantity}"
            )

        product.stock_quantity -= quantity
        product.updated_at = datetime.utcnow()

        return await self._repository.update(product)

    async def increase_stock(self, product_id: str, quantity: int) -> Product:
        """
        Increase stock for a product.

        Validates that quantity is positive.
        """
        product = await self._repository.find_by_id(product_id)
        if product is None:
            raise NotFoundError(f"Product with id '{product_id}' not found")

        if quantity <= 0:
            raise ValidationError("Increase quantity must be positive")

        product.stock_quantity += quantity
        product.updated_at = datetime.utcnow()

        return await self._repository.update(product)
