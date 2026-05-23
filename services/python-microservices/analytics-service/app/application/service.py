import logging
from typing import Optional
from datetime import date
from decimal import Decimal
from collections import defaultdict

from app.domain.models import Event, DailyAggregation
from app.domain.interfaces import AnalyticsRepositoryProtocol

logger = logging.getLogger(__name__)

# Map event types to source services
EVENT_SOURCE_MAP = {
    "USER_CREATED": "user-service",
    "USER_UPDATED": "user-service",
    "USER_DELETED": "user-service",
    "PRODUCT_CREATED": "product-service",
    "PRODUCT_UPDATED": "product-service",
    "PRODUCT_DELETED": "product-service",
    "REVIEW_ADDED": "product-service",
    "ITEM_ADDED": "cart-service",
    "ITEM_REMOVED": "cart-service",
    "CART_CHECKED_OUT": "cart-service",
    "ORDER_CREATED": "order-service",
    "ORDER_CONFIRMED": "order-service",
    "ORDER_FAILED": "order-service",
    "ORDER_CANCELLED": "order-service",
    "PAYMENT_COMPLETED": "payment-service",
    "PAYMENT_FAILED": "payment-service",
    "PAYMENT_REFUNDED": "payment-service",
    "STOCK_RESERVED": "inventory-service",
    "STOCK_RELEASED": "inventory-service",
}


class AnalyticsService:
    def __init__(self, repository: AnalyticsRepositoryProtocol):
        self.repository = repository
        # In-memory counters for real-time metrics
        self.counters: dict[str, int] = defaultdict(int)
        self.revenue_today: Decimal = Decimal("0.00")

    async def handle_event(self, event_data: dict) -> None:
        """Handle incoming events from Kafka and store for analytics."""
        event_type = event_data.get("event", "UNKNOWN")
        source = EVENT_SOURCE_MAP.get(event_type, "unknown")

        logger.info(f"Analytics received: {event_type} from {source}")

        # Store event
        event = Event(
            event_type=event_type,
            source_service=source,
            payload=event_data,
        )
        await self.repository.save_event(event)

        # Update counters
        self.counters[event_type] += 1
        self.counters[f"source:{source}"] += 1

        # Track revenue
        if event_type == "PAYMENT_COMPLETED":
            amount = Decimal(event_data.get("amount", "0"))
            self.revenue_today += amount

    async def get_events(self, event_type: Optional[str] = None, limit: int = 100) -> list[Event]:
        return await self.repository.get_events(event_type, limit)

    async def get_metrics_summary(self) -> dict:
        """Get real-time metrics summary."""
        return {
            "counters": dict(self.counters),
            "revenue_today": str(self.revenue_today),
            "total_events": sum(
                v for k, v in self.counters.items() if not k.startswith("source:")
            ),
        }

    async def get_event_counts_by_type(self) -> dict[str, int]:
        """Get event counts grouped by type."""
        return {
            k: v for k, v in self.counters.items() if not k.startswith("source:")
        }

    async def get_event_counts_by_source(self) -> dict[str, int]:
        """Get event counts grouped by source service."""
        return {
            k.replace("source:", ""): v
            for k, v in self.counters.items() if k.startswith("source:")
        }

    async def get_daily_summary(self, target_date: Optional[date] = None) -> dict:
        """Get daily aggregation summary."""
        target = target_date or date.today()
        agg = await self.repository.get_daily_aggregation(target)
        if agg:
            return {
                "date": str(agg.date),
                "total_orders": agg.total_orders,
                "total_revenue": str(agg.total_revenue),
                "total_users": agg.total_users,
            }
        return {
            "date": str(target),
            "total_orders": self.counters.get("ORDER_CREATED", 0),
            "total_revenue": str(self.revenue_today),
            "total_users": self.counters.get("USER_CREATED", 0),
        }
