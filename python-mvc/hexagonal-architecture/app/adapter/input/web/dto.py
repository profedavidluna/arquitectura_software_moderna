"""
Input Adapter DTOs: Web Data Transfer Objects.

These Pydantic models define the contract between the HTTP layer and the
application. They handle serialization, validation, and documentation
for the REST API.

DTOs belong to the adapter layer and translate between the external world
(HTTP requests/responses) and the domain model.
"""
from __future__ import annotations

from datetime import datetime
from typing import Any, Optional

from pydantic import BaseModel, Field


class CreateProductRequest(BaseModel):
    """Request body for creating a new product."""

    name: str = Field(..., min_length=1, max_length=255, description="Product name")
    description: str = Field(..., max_length=1000, description="Product description")
    price: float = Field(..., gt=0, description="Product price (must be > 0)")
    category: str = Field(..., min_length=1, max_length=100, description="Product category")
    stock_quantity: int = Field(..., ge=0, description="Initial stock quantity")
    sku: str = Field(..., min_length=1, max_length=50, description="Stock Keeping Unit")

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "name": "Wireless Mouse",
                    "description": "Ergonomic wireless mouse with USB receiver",
                    "price": 29.99,
                    "category": "Electronics",
                    "stock_quantity": 150,
                    "sku": "ELEC-WM-001",
                }
            ]
        }
    }


class UpdateProductRequest(BaseModel):
    """Request body for updating an existing product."""

    name: Optional[str] = Field(None, min_length=1, max_length=255)
    description: Optional[str] = Field(None, max_length=1000)
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

    model_config = {"from_attributes": True}


class PaginatedResponse(BaseModel):
    """Paginated response containing a list of products."""

    items: list[ProductResponse]
    total: int
    page: int
    size: int
    total_pages: int


class ErrorResponse(BaseModel):
    """Standard error response."""

    error: str
    message: str
    status_code: int
    details: Optional[Any] = None
