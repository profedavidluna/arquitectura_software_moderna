from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional
import uuid


@dataclass
class InventoryItem:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    product_id: str = ""
    quantity: int = 0
    reserved: int = 0
    warehouse_location: Optional[str] = None
    updated_at: datetime = field(default_factory=datetime.now)

    @property
    def available(self) -> int:
        return self.quantity - self.reserved


@dataclass
class Reservation:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    order_id: str = ""
    product_id: str = ""
    quantity: int = 0
    status: str = "RESERVED"
    expires_at: Optional[datetime] = None
    created_at: datetime = field(default_factory=datetime.now)
