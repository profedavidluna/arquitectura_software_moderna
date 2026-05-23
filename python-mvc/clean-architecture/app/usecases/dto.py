"""
@layer Use Cases - Data Transfer Objects
@description DTOs for use case input/output.

These DTOs define the data structures that flow between the use cases
and the interface adapters. They are simple data containers with no
business logic.

In Clean Architecture, use case DTOs ensure that entities are not
exposed directly to outer layers, maintaining the boundary.
"""

from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal
from typing import Optional


@dataclass
class CreateProductInput:
    """Input data for creating a product."""
    name: str
    description: str
    price: Decimal
    category: str
    stock_quantity: int
    sku: str


@dataclass
class UpdateProductInput:
    """Input data for updating a product."""
    product_id: str
    name: Optional[str] = None
    description: Optional[str] = None
    price: Optional[Decimal] = None
    category: Optional[str] = None
    stock_quantity: Optional[int] = None
    sku: Optional[str] = None


@dataclass
class ProductOutput:
    """Output data representing a product."""
    id: str
    name: str
    description: str
    price: Decimal
    category: str
    stock_quantity: int
    sku: str
    active: bool
    created_at: datetime
    updated_at: datetime


@dataclass
class ProductListOutput:
    """Output data for a paginated list of products."""
    products: list[ProductOutput]
    total: int
    page: int
    size: int
