from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class EventResponse(BaseModel):
    id: str
    event_type: str
    source_service: str
    payload: Optional[dict] = None
    created_at: datetime


class MetricsSummaryResponse(BaseModel):
    counters: dict[str, int]
    revenue_today: str
    total_events: int


class DailySummaryResponse(BaseModel):
    date: str
    total_orders: int
    total_revenue: str
    total_users: int
