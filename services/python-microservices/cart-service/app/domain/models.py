from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional
from decimal import Decimal
import uuid


@dataclass
class CartItem:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    cart_id: str = ""
    product_id: str = ""
    product_name: str = ""
    price: Decimal = Decimal("0.00")
    quantity: int = 1
    created_at: datetime = field(default_factory=datetime.now)


@dataclass
class Cart:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    user_id: str = ""
    status: str = "ACTIVE"
    coupon_code: Optional[str] = None
    discount_percent: Decimal = Decimal("0.00")
    items: list[CartItem] = field(default_factory=list)
    created_at: datetime = field(default_factory=datetime.now)
    updated_at: datetime = field(default_factory=datetime.now)

    @property
    def subtotal(self) -> Decimal:
        return sum(item.price * item.quantity for item in self.items)

    @property
    def total(self) -> Decimal:
        discount = self.subtotal * (self.discount_percent / Decimal("100"))
        return self.subtotal - discount
