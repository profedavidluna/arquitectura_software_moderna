"""
Inventory DTOs (Data Transfer Objects)
========================================
Pydantic models for the Inventory Service API.
"""

from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, Field


class CreateInventoryRequest(BaseModel):
    """Request body for creating a new inventory entry."""

    product_id: str = Field(..., description="Product UUID")
    product_name: str = Field(..., min_length=1, description="Product name")
    initial_quantity: int = Field(default=0, ge=0, description="Initial stock quantity")


class AddStockRequest(BaseModel):
    """Request body for adding stock to an inventory item."""

    quantity: int = Field(..., gt=0, description="Quantity to add (must be positive)")


class InventoryResponse(BaseModel):
    """Response body representing an inventory item."""

    id: UUID
    product_id: UUID
    product_name: str
    quantity_available: int
    quantity_reserved: int
    total_stock: int
    updated_at: datetime

    class Config:
        from_attributes = True
