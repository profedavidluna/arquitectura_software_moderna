"""
Input Port: Product Service Port.

This port defines the interface that the application offers to the outside world
(driving/primary adapters). It represents the USE CASES available in the system.

In Hexagonal Architecture, input ports are implemented by domain services and
called by input adapters (e.g., REST controllers, CLI commands, message consumers).
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Optional

from app.domain.model.product import Product


@dataclass(frozen=True)
class CreateProductCommand:
    """Command object for creating a new product."""

    name: str
    description: str
    price: float
    category: str
    stock_quantity: int
    sku: str


@dataclass(frozen=True)
class UpdateProductCommand:
    """Command object for updating an existing product."""

    name: Optional[str] = None
    description: Optional[str] = None
    price: Optional[float] = None
    category: Optional[str] = None
    stock_quantity: Optional[int] = None
    sku: Optional[str] = None
    active: Optional[bool] = None


@dataclass(frozen=True)
class SearchCriteria:
    """Criteria for searching products."""

    query: Optional[str] = None
    category: Optional[str] = None
    min_price: Optional[float] = None
    max_price: Optional[float] = None


@dataclass(frozen=True)
class PaginatedResult:
    """Paginated result containing items and metadata."""

    items: list[Product] = field(default_factory=list)
    total: int = 0
    page: int = 1
    size: int = 10
    total_pages: int = 0


class ProductServicePort(ABC):
    """
    Input port defining the product catalog use cases.

    This interface is implemented by the domain service and used by
    input adapters (controllers, CLI, etc.) to interact with the domain.
    """

    @abstractmethod
    async def create_product(self, command: CreateProductCommand) -> Product:
        """Create a new product in the catalog."""
        ...

    @abstractmethod
    async def get_product_by_id(self, product_id: str) -> Product:
        """Retrieve a product by its unique identifier."""
        ...

    @abstractmethod
    async def list_products(self, page: int = 1, size: int = 10) -> PaginatedResult:
        """List products with pagination."""
        ...

    @abstractmethod
    async def search_products(self, criteria: SearchCriteria) -> list[Product]:
        """Search products based on given criteria."""
        ...

    @abstractmethod
    async def update_product(
        self, product_id: str, command: UpdateProductCommand
    ) -> Product:
        """Update an existing product."""
        ...

    @abstractmethod
    async def delete_product(self, product_id: str) -> None:
        """Delete a product by its identifier."""
        ...

    @abstractmethod
    async def decrease_stock(self, product_id: str, quantity: int) -> Product:
        """Decrease the stock of a product."""
        ...

    @abstractmethod
    async def increase_stock(self, product_id: str, quantity: int) -> Product:
        """Increase the stock of a product."""
        ...
