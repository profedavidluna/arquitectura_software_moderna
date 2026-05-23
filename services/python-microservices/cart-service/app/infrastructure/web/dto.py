from pydantic import BaseModel
from typing import Optional
from datetime import datetime
from decimal import Decimal


class AddItemRequest(BaseModel):
    product_id: str
    quantity: int = 1


class ApplyCouponRequest(BaseModel):
    coupon_code: str


class CartItemResponse(BaseModel):
    id: str
    product_id: str
    product_name: str
    price: Decimal
    quantity: int


class CartResponse(BaseModel):
    id: str
    user_id: str
    status: str
    coupon_code: Optional[str] = None
    discount_percent: Decimal
    items: list[CartItemResponse]
    subtotal: Decimal
    total: Decimal
    created_at: datetime
    updated_at: datetime
