from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional
from decimal import Decimal
import uuid


@dataclass
class Category:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    name: str = ""
    description: Optional[str] = None
    parent_id: Optional[str] = None
    created_at: datetime = field(default_factory=datetime.now)


@dataclass
class Product:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    name: str = ""
    description: Optional[str] = None
    price: Decimal = Decimal("0.00")
    category_id: Optional[str] = None
    image_url: Optional[str] = None
    status: str = "ACTIVE"
    created_at: datetime = field(default_factory=datetime.now)
    updated_at: datetime = field(default_factory=datetime.now)


@dataclass
class ProductReview:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    product_id: str = ""
    user_id: str = ""
    rating: int = 5
    comment: Optional[str] = None
    created_at: datetime = field(default_factory=datetime.now)
