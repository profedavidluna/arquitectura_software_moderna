import pytest
from unittest.mock import AsyncMock
from decimal import Decimal
from app.application.service import AnalyticsService
from app.domain.models import Event


@pytest.fixture
def mock_repository():
    repo = AsyncMock()
    repo.save_event.return_value = Event(
        id="evt-1", event_type="TEST", source_service="test"
    )
    return repo


@pytest.fixture
def analytics_service(mock_repository):
    return AnalyticsService(mock_repository)


@pytest.mark.asyncio
async def test_handle_order_event(analytics_service, mock_repository):
    event = {"event": "ORDER_CREATED", "orderId": "order-1", "userId": "user-1"}

    await analytics_service.handle_event(event)

    mock_repository.save_event.assert_called_once()
    assert analytics_service.counters["ORDER_CREATED"] == 1
    assert analytics_service.counters["source:order-service"] == 1


@pytest.mark.asyncio
async def test_handle_payment_event_tracks_revenue(analytics_service):
    event = {"event": "PAYMENT_COMPLETED", "paymentId": "p1", "orderId": "o1", "amount": "99.99"}

    await analytics_service.handle_event(event)

    assert analytics_service.revenue_today == Decimal("99.99")


@pytest.mark.asyncio
async def test_multiple_events_accumulate(analytics_service):
    events = [
        {"event": "USER_CREATED", "userId": "u1", "email": "a@b.com"},
        {"event": "USER_CREATED", "userId": "u2", "email": "c@d.com"},
        {"event": "ORDER_CREATED", "orderId": "o1", "userId": "u1"},
    ]

    for event in events:
        await analytics_service.handle_event(event)

    assert analytics_service.counters["USER_CREATED"] == 2
    assert analytics_service.counters["ORDER_CREATED"] == 1


@pytest.mark.asyncio
async def test_get_metrics_summary(analytics_service):
    await analytics_service.handle_event(
        {"event": "PAYMENT_COMPLETED", "amount": "50.00", "orderId": "o1", "paymentId": "p1"}
    )
    await analytics_service.handle_event(
        {"event": "ORDER_CREATED", "orderId": "o2", "userId": "u1"}
    )

    summary = await analytics_service.get_metrics_summary()

    assert summary["total_events"] == 2
    assert summary["revenue_today"] == "50.00"


@pytest.mark.asyncio
async def test_get_event_counts_by_source(analytics_service):
    await analytics_service.handle_event({"event": "USER_CREATED", "userId": "u1", "email": "a@b.com"})
    await analytics_service.handle_event({"event": "ORDER_CREATED", "orderId": "o1", "userId": "u1"})

    by_source = await analytics_service.get_event_counts_by_source()

    assert by_source["user-service"] == 1
    assert by_source["order-service"] == 1
