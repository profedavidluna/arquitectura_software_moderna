from fastapi import APIRouter, Request, Query
from typing import Optional

from app.infrastructure.web.dto import (
    EventResponse, MetricsSummaryResponse, DailySummaryResponse,
)


def create_router() -> APIRouter:
    router = APIRouter()

    @router.get("/events", response_model=list[EventResponse])
    async def get_events(
        request: Request,
        event_type: Optional[str] = None,
        limit: int = Query(default=100, ge=1, le=500),
    ):
        service = request.app.state.service
        events = await service.get_events(event_type, limit)
        return [
            EventResponse(
                id=e.id, event_type=e.event_type,
                source_service=e.source_service,
                payload=e.payload, created_at=e.created_at,
            )
            for e in events
        ]

    @router.get("/metrics", response_model=MetricsSummaryResponse)
    async def get_metrics(request: Request):
        service = request.app.state.service
        summary = await service.get_metrics_summary()
        return MetricsSummaryResponse(**summary)

    @router.get("/metrics/by-type")
    async def get_metrics_by_type(request: Request):
        service = request.app.state.service
        return await service.get_event_counts_by_type()

    @router.get("/metrics/by-source")
    async def get_metrics_by_source(request: Request):
        service = request.app.state.service
        return await service.get_event_counts_by_source()

    @router.get("/daily", response_model=DailySummaryResponse)
    async def get_daily_summary(request: Request):
        service = request.app.state.service
        summary = await service.get_daily_summary()
        return DailySummaryResponse(**summary)

    return router
