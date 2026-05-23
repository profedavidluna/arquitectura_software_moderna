from typing import Protocol
from app.domain.models import Notification


class NotificationSenderProtocol(Protocol):
    async def send(self, notification: Notification) -> bool: ...
