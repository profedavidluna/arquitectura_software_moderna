from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional
import uuid


@dataclass
class Notification:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    type: str = ""  # EMAIL, SMS, PUSH
    recipient: str = ""
    subject: str = ""
    body: str = ""
    status: str = "PENDING"  # PENDING, SENT, FAILED
    source_event: str = ""
    metadata: Optional[dict] = None
    created_at: datetime = field(default_factory=datetime.now)
    sent_at: Optional[datetime] = None
