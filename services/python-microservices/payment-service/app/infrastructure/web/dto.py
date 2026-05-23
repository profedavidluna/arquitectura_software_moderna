from pydantic import BaseModel
from typing import Optional
from datetime import datetime
from decimal import Decimal


class CreatePaymentRequest(BaseModel):
    orderId: str
    userId: str
    amount: Decimal
    method: str = "CREDIT_CARD"
    currency: str = "USD"


class RefundRequest(BaseModel):
    amount: Optional[Decimal] = None
    reason: Optional[str] = None


class PaymentResponse(BaseModel):
    id: str
    order_id: str
    user_id: str
    amount: Decimal
    currency: str
    method: str
    status: str
    transaction_id: Optional[str] = None
    failure_reason: Optional[str] = None
    retry_count: int
    created_at: datetime
    updated_at: datetime


class RefundResponse(BaseModel):
    id: str
    payment_id: str
    amount: Decimal
    reason: Optional[str] = None
    status: str
    created_at: datetime
