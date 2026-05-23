"""
Order DTOs (Data Transfer Objects)
====================================
Pydantic models for the Order Service API.
"""

from datetime import datetime
from decimal import Decimal
from uuid import UUID

from pydantic import BaseModel, Field


class OrderItemRequest(BaseModel):
    """Request body for a single order item."""

    product_id: str = Field(..., description="Product UUID")
    product_name: str = Field(default="", description="Product name for display")
    quantity: int = Field(..., gt=0, description="Quantity to order")
    unit_price: float = Field(..., gt=0, description="Price per unit")


class CreateOrderRequest(BaseModel):
    """Request body for creating a new order."""

    user_id: str = Field(..., description="User UUID placing the order")
    items: list[OrderItemRequest] = Field(
        ..., min_length=1, description="Order items (at least one required)"
    )


class CancelOrderRequest(BaseModel):
    """Request body for cancelling an order."""

    reason: str = Field(default="User requested cancellation", description="Cancellation reason")


class OrderItemResponse(BaseModel):
    """Response body for a single order item."""

    id: UUID
    product_id: UUID
    product_name: str
    quantity: int
    unit_price: float
    subtotal: float


class OrderResponse(BaseModel):
    """Response body representing an order."""

    id: UUID
    user_id: UUID
    status: str
    total_amount: float
    items: list[OrderItemResponse]
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
