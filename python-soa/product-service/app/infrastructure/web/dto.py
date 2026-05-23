"""
Product DTOs (Data Transfer Objects)
======================================
Pydantic models for API request/response serialization.

Design Pattern: DTO (Data Transfer Object)
- Separates API contract from domain model
- Provides validation at the API boundary
- Allows API and domain to evolve independently

Pydantic v2 Features Used:
- Field validation with constraints
- Automatic JSON serialization
- OpenAPI schema generation
"""

from datetime import datetime
from decimal import Decimal
from uuid import UUID

from pydantic import BaseModel, Field


class CreateProductRequest(BaseModel):
    """Request body for creating a new product."""

    name: str = Field(..., min_length=1, max_length=255, description="Product name")
    description: str = Field(default="", description="Product description")
    price: float = Field(..., gt=0, description="Product price (must be positive)")
    category: str = Field(..., min_length=1, max_length=100, description="Product category")
    sku: str = Field(..., min_length=1, max_length=100, description="Stock Keeping Unit")


class UpdateProductRequest(BaseModel):
    """Request body for updating a product (partial update)."""

    name: str | None = Field(None, min_length=1, max_length=255)
    description: str | None = None
    price: float | None = Field(None, gt=0)
    category: str | None = Field(None, min_length=1, max_length=100)


class ProductResponse(BaseModel):
    """Response body representing a product."""

    id: UUID
    name: str
    description: str
    price: float
    category: str
    sku: str
    active: bool
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
