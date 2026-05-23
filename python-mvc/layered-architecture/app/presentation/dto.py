"""
Presentation Layer: Data Transfer Objects (DTOs).

Pydantic models used for request validation and response serialization.
These DTOs define the API contract for external consumers.

In Layered Architecture, DTOs live in the Presentation Layer and are used
to decouple the external API format from the internal data models.
The controller maps between DTOs and the Business Layer's data models.
"""
from __future__ import annotations

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


class CreateProductRequest(BaseModel):
    """Request body for creating a new product."""

    name: str = Field(..., min_length=1, max_length=255)
    description: str = Field(default="", max_length=2000)
    price: float = Field(..., gt=0)
    category: str = Field(..., min_length=1, max_length=100)
    stock_quantity: int = Field(default=0, ge=0)
    sku: str = Field(..., min_length=1, max_length=50)


class UpdateProductRequest(BaseModel):
    """Request body for updating an existing product. All fields optional."""

    name: Optional[str] = Field(None, min_length=1, max_length=255)
    description: Optional[str] = Field(None, max_length=2000)
    price: Optional[float] = Field(None, gt=0)
    category: Optional[str] = Field(None, min_length=1, max_length=100)
    stock_quantity: Optional[int] = Field(None, ge=0)
    sku: Optional[str] = Field(None, min_length=1, max_length=50)
    active: Optional[bool] = None


class ProductResponse(BaseModel):
    """Response body representing a product."""

    id: str
    name: str
    description: str
    price: float
    category: str
    stock_quantity: int
    sku: str
    active: bool
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


class PaginatedResponse(BaseModel):
    """Paginated response containing a list of products."""

    items: list[ProductResponse]
    total: int
    page: int
    size: int
    total_pages: int
