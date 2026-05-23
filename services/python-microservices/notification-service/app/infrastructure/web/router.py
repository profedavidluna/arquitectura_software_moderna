from fastapi import APIRouter, Request, Query

from app.infrastructure.web.dto import NotificationResponse, StatsResponse


def create_router() -> APIRouter:
    router = APIRouter()

    @router.get("", response_model=list[NotificationResponse])
    async def get_notifications(
        request: Request,
        limit: int = Query(default=50, ge=1, le=200),
    ):
        service = request.app.state.service
        notifications = service.get_recent_notifications(limit)
        return [
            NotificationResponse(
                id=n.id, type=n.type, recipient=n.recipient,
                subject=n.subject, body=n.body, status=n.status,
                source_event=n.source_event, created_at=n.created_at,
                sent_at=n.sent_at,
            )
            for n in notifications
        ]

    @router.get("/stats", response_model=StatsResponse)
    async def get_stats(request: Request):
        service = request.app.state.service
        stats = service.get_stats()
        return StatsResponse(**stats)

    return router
