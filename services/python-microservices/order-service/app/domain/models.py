from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional
from decimal import Decimal
import uuid


@dataclass
class OrderItem:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    order_id: str = ""
    product_id: str = ""
    product_name: str = ""
    price: Decimal = Decimal("0.00")
    quantity: int = 1


@dataclass
class Order:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    user_id: str = ""
    status: str = "PENDING"
    total_amount: Decimal = Decimal("0.00")
    shipping_address: Optional[dict] = None
    saga_status: str = "STARTED"
    items: list[OrderItem] = field(default_factory=list)
    created_at: datetime = field(default_factory=datetime.now)
    updated_at: datetime = field(default_factory=datetime.now)


@dataclass
class SagaStep:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    order_id: str = ""
    step_name: str = ""
    status: str = "PENDING"
    request_payload: Optional[dict] = None
    response_payload: Optional[dict] = None
    executed_at: Optional[datetime] = None
    compensated_at: Optional[datetime] = None
