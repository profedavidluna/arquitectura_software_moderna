from dataclasses import dataclass, field
from datetime import datetime, date
from typing import Optional
from decimal import Decimal
import uuid


@dataclass
class Event:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    event_type: str = ""
    source_service: str = ""
    payload: Optional[dict] = None
    created_at: datetime = field(default_factory=datetime.now)


@dataclass
class Metric:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    metric_name: str = ""
    metric_value: Decimal = Decimal("0")
    dimensions: Optional[dict] = None
    period_start: datetime = field(default_factory=datetime.now)
    period_end: datetime = field(default_factory=datetime.now)
    created_at: datetime = field(default_factory=datetime.now)


@dataclass
class DailyAggregation:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    date: date = field(default_factory=date.today)
    total_orders: int = 0
    total_revenue: Decimal = Decimal("0.00")
    total_users: int = 0
    top_products: Optional[dict] = None
    created_at: datetime = field(default_factory=datetime.now)
