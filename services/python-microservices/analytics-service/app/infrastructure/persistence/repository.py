import json
from typing import Optional
from datetime import date
from decimal import Decimal
from app.domain.models import Event, Metric, DailyAggregation
from app.infrastructure.persistence.database import Database


class AnalyticsRepository:
    def __init__(self, db: Database):
        self.db = db

    async def save_event(self, event: Event) -> Event:
        query = """
            INSERT INTO events (id, event_type, source_service, payload)
            VALUES ($1, $2, $3, $4) RETURNING *
        """
        row = await self.db.fetch_one(
            query, event.id, event.event_type, event.source_service,
            json.dumps(event.payload) if event.payload else None,
        )
        return Event(
            id=str(row["id"]), event_type=row["event_type"],
            source_service=row["source_service"],
            payload=json.loads(row["payload"]) if row["payload"] else None,
            created_at=row["created_at"],
        )

    async def get_events(self, event_type: Optional[str] = None, limit: int = 100) -> list[Event]:
        if event_type:
            rows = await self.db.fetch_all(
                "SELECT * FROM events WHERE event_type = $1 ORDER BY created_at DESC LIMIT $2",
                event_type, limit,
            )
        else:
            rows = await self.db.fetch_all(
                "SELECT * FROM events ORDER BY created_at DESC LIMIT $1", limit
            )
        return [
            Event(
                id=str(r["id"]), event_type=r["event_type"],
                source_service=r["source_service"],
                payload=json.loads(r["payload"]) if r["payload"] else None,
                created_at=r["created_at"],
            )
            for r in rows
        ]

    async def save_metric(self, metric: Metric) -> Metric:
        query = """
            INSERT INTO metrics (id, metric_name, metric_value, dimensions, period_start, period_end)
            VALUES ($1, $2, $3, $4, $5, $6) RETURNING *
        """
        row = await self.db.fetch_one(
            query, metric.id, metric.metric_name, metric.metric_value,
            json.dumps(metric.dimensions) if metric.dimensions else None,
            metric.period_start, metric.period_end,
        )
        return metric

    async def get_metrics(self, metric_name: Optional[str] = None) -> list[Metric]:
        if metric_name:
            rows = await self.db.fetch_all(
                "SELECT * FROM metrics WHERE metric_name = $1 ORDER BY created_at DESC",
                metric_name,
            )
        else:
            rows = await self.db.fetch_all(
                "SELECT * FROM metrics ORDER BY created_at DESC LIMIT 100"
            )
        return [
            Metric(
                id=str(r["id"]), metric_name=r["metric_name"],
                metric_value=Decimal(str(r["metric_value"])),
                dimensions=json.loads(r["dimensions"]) if r["dimensions"] else None,
                period_start=r["period_start"], period_end=r["period_end"],
                created_at=r["created_at"],
            )
            for r in rows
        ]

    async def get_daily_aggregation(self, target_date: date) -> Optional[DailyAggregation]:
        row = await self.db.fetch_one(
            "SELECT * FROM daily_aggregations WHERE date = $1", target_date
        )
        if not row:
            return None
        return DailyAggregation(
            id=str(row["id"]), date=row["date"],
            total_orders=row["total_orders"],
            total_revenue=Decimal(str(row["total_revenue"])),
            total_users=row["total_users"],
            top_products=json.loads(row["top_products"]) if row["top_products"] else None,
            created_at=row["created_at"],
        )

    async def save_daily_aggregation(self, agg: DailyAggregation) -> DailyAggregation:
        query = """
            INSERT INTO daily_aggregations (id, date, total_orders, total_revenue, total_users, top_products)
            VALUES ($1, $2, $3, $4, $5, $6)
            ON CONFLICT (date) DO UPDATE SET
                total_orders = $3, total_revenue = $4, total_users = $5, top_products = $6
            RETURNING *
        """
        await self.db.fetch_one(
            query, agg.id, agg.date, agg.total_orders,
            agg.total_revenue, agg.total_users,
            json.dumps(agg.top_products) if agg.top_products else None,
        )
        return agg
