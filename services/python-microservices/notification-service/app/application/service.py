import logging
from datetime import datetime
from collections import deque

from app.domain.models import Notification

logger = logging.getLogger(__name__)

# Notification templates
TEMPLATES = {
    "USER_CREATED": {
        "subject": "Welcome to our platform!",
        "body": "Hello {email}, your account has been created successfully.",
    },
    "ORDER_CREATED": {
        "subject": "Order Confirmation",
        "body": "Your order {orderId} has been placed successfully.",
    },
    "ORDER_CONFIRMED": {
        "subject": "Order Confirmed",
        "body": "Your order {orderId} has been confirmed and is being processed.",
    },
    "ORDER_CANCELLED": {
        "subject": "Order Cancelled",
        "body": "Your order {orderId} has been cancelled.",
    },
    "PAYMENT_COMPLETED": {
        "subject": "Payment Received",
        "body": "Payment of ${amount} for order {orderId} has been processed.",
    },
    "PAYMENT_FAILED": {
        "subject": "Payment Failed",
        "body": "Payment for order {orderId} has failed. Please try again.",
    },
    "PAYMENT_REFUNDED": {
        "subject": "Refund Processed",
        "body": "A refund of ${amount} has been processed for payment {paymentId}.",
    },
    "LOW_STOCK_ALERT": {
        "subject": "Low Stock Alert",
        "body": "Product {productId} is running low on stock.",
    },
}


class NotificationService:
    def __init__(self):
        self.notifications: deque[Notification] = deque(maxlen=1000)
        self.stats = {
            "total_received": 0,
            "total_sent": 0,
            "total_failed": 0,
        }

    async def handle_event(self, event: dict) -> None:
        """Handle incoming events from Kafka and create notifications."""
        event_type = event.get("event", "UNKNOWN")
        self.stats["total_received"] += 1

        logger.info(f"Received event: {event_type} - {event}")

        template = TEMPLATES.get(event_type)
        if not template:
            logger.debug(f"No template for event type: {event_type}")
            return

        try:
            subject = template["subject"]
            body = template["body"].format(**event)
        except KeyError:
            body = template["body"]
            subject = template["subject"]

        notification = Notification(
            type="EMAIL",
            recipient=event.get("email", event.get("userId", "system")),
            subject=subject,
            body=body,
            source_event=event_type,
            metadata=event,
            status="SENT",
            sent_at=datetime.now(),
        )

        # Simulate sending (in production, integrate with email service)
        await self._send_notification(notification)
        self.notifications.append(notification)

    async def _send_notification(self, notification: Notification) -> None:
        """Simulate sending a notification."""
        logger.info(
            f"[NOTIFICATION] To: {notification.recipient} | "
            f"Subject: {notification.subject} | Body: {notification.body}"
        )
        self.stats["total_sent"] += 1

    def get_recent_notifications(self, limit: int = 50) -> list[Notification]:
        """Get recent notifications."""
        items = list(self.notifications)
        items.reverse()
        return items[:limit]

    def get_stats(self) -> dict:
        return self.stats.copy()
