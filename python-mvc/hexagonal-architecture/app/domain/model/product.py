"""
Domain Model: Product Entity.

This is the core of the hexagonal architecture. The Product entity contains
business rules and is completely independent of any framework, database,
or external technology. It has ZERO framework dependencies.

The entity is implemented as a frozen (immutable) dataclass. All mutation
methods return new instances, preserving immutability and making the domain
model safe for concurrent use.
"""
from __future__ import annotations

from dataclasses import dataclass, replace
from datetime import datetime
from typing import Optional
from uuid import uuid4


@dataclass(frozen=True)
class Product:
    """
    Product domain entity.

    Represents a product in the catalog with all its business rules.
    This class is immutable - all state changes produce new instances.
    """

    id: str
    name: str
    description: str
    price: float
    category: str
    stock_quantity: int
    sku: str
    active: bool = True
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    @staticmethod
    def validate_price(price: float) -> None:
        """Validate that price is positive."""
        if price <= 0:
            raise InvalidPriceError(f"Price must be greater than 0, got {price}")

    @staticmethod
    def validate_stock(quantity: int) -> None:
        """Validate that stock quantity is non-negative."""
        if quantity < 0:
            raise InvalidStockError(f"Stock quantity cannot be negative, got {quantity}")

    def decrease_stock(self, quantity: int) -> Product:
        """
        Decrease stock by the given quantity.

        Returns a new Product instance with updated stock.
        Raises InsufficientStockError if quantity exceeds current stock.
        """
        if quantity <= 0:
            raise InvalidStockError("Decrease quantity must be positive")
        if quantity > self.stock_quantity:
            raise InsufficientStockError(
                f"Cannot decrease stock by {quantity}. "
                f"Current stock: {self.stock_quantity}"
            )
        return replace(
            self,
            stock_quantity=self.stock_quantity - quantity,
            updated_at=datetime.utcnow(),
        )

    def increase_stock(self, quantity: int) -> Product:
        """
        Increase stock by the given quantity.

        Returns a new Product instance with updated stock.
        """
        if quantity <= 0:
            raise InvalidStockError("Increase quantity must be positive")
        return replace(
            self,
            stock_quantity=self.stock_quantity + quantity,
            updated_at=datetime.utcnow(),
        )

    def deactivate(self) -> Product:
        """
        Deactivate the product.

        Returns a new Product instance marked as inactive.
        """
        return replace(self, active=False, updated_at=datetime.utcnow())

    @staticmethod
    def generate_id() -> str:
        """Generate a new unique identifier for a product."""
        return str(uuid4())


# --- Domain Exceptions ---


class ProductDomainError(Exception):
    """Base exception for product domain errors."""
    pass


class InvalidPriceError(ProductDomainError):
    """Raised when a product price is invalid."""
    pass


class InvalidStockError(ProductDomainError):
    """Raised when a stock quantity is invalid."""
    pass


class InsufficientStockError(ProductDomainError):
    """Raised when there is not enough stock to fulfill an operation."""
    pass


class ProductNotFoundError(ProductDomainError):
    """Raised when a product cannot be found."""
    pass


class DuplicateSkuError(ProductDomainError):
    """Raised when attempting to create a product with an existing SKU."""
    pass
