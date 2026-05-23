from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class AddStockRequest(BaseModel):
    product_id: str
    quantity: int
    warehouse_location: Optional[str] = None


class ReserveItemRequest(BaseModel):
    productId: str
    quantity: int


class ReserveRequest(BaseModel):
    orderId: str
    items: list[ReserveItemRequest]


class ReleaseRequest(BaseModel):
    orderId: str
    items: list[ReserveItemRequest]


class InventoryResponse(BaseModel):
    id: str
    product_id: str
    quantity: int
    reserved: int
    available: int
    warehouse_location: Optional[str] = None
    updated_at: datetime


class ReservationResponse(BaseModel):
    id: str
    order_id: str
    product_id: str
    quantity: int
    status: str
    created_at: datetime
