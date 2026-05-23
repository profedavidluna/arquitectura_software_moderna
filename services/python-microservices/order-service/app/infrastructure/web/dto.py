from pydantic import BaseModel
from typing import Optional
from datetime import datetime
from decimal import Decimal


class OrderItemRequest(BaseModel):
    productId: str
    productName: str
    price: Decimal
    quantity: int


class CreateOrderRequest(BaseModel):
    user_id: str
    cart_id: str
    items: list[OrderItemRequest]
    total_amount: Decimal
    shipping_address: Optional[dict] = None


class OrderItemResponse(BaseModel):
    id: str
    product_id: str
    product_name: str
    price: Decimal
    quantity: int


class OrderResponse(BaseModel):
    id: str
    user_id: str
    status: str
    total_amount: Decimal
    shipping_address: Optional[dict] = None
    saga_status: str
    items: list[OrderItemResponse]
    created_at: datetime
    updated_at: datetime
