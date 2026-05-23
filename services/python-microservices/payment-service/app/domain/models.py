from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional
from decimal import Decimal
import uuid


@dataclass
class Payment:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    order_id: str = ""
    user_id: str = ""
    amount: Decimal = Decimal("0.00")
    currency: str = "USD"
    method: str = "CREDIT_CARD"
    status: str = "PENDING"
    transaction_id: Optional[str] = None
    failure_reason: Optional[str] = None
    retry_count: int = 0
    created_at: datetime = field(default_factory=datetime.now)
    updated_at: datetime = field(default_factory=datetime.now)


@dataclass
class Refund:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    payment_id: str = ""
    amount: Decimal = Decimal("0.00")
    reason: Optional[str] = None
    status: str = "PENDING"
    created_at: datetime = field(default_factory=datetime.now)
