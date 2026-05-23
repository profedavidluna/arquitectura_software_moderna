from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class NotificationResponse(BaseModel):
    id: str
    type: str
    recipient: str
    subject: str
    body: str
    status: str
    source_event: str
    created_at: datetime
    sent_at: Optional[datetime] = None


class StatsResponse(BaseModel):
    total_received: int
    total_sent: int
    total_failed: int
