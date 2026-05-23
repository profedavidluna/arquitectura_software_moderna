import pytest
from app.application.service import NotificationService


@pytest.fixture
def notification_service():
    return NotificationService()


@pytest.mark.asyncio
async def test_handle_user_created_event(notification_service):
    event = {"event": "USER_CREATED", "userId": "user-1", "email": "test@example.com"}

    await notification_service.handle_event(event)

    notifications = notification_service.get_recent_notifications()
    assert len(notifications) == 1
    assert notifications[0].subject == "Welcome to our platform!"
    assert "test@example.com" in notifications[0].body


@pytest.mark.asyncio
async def test_handle_order_confirmed_event(notification_service):
    event = {"event": "ORDER_CONFIRMED", "orderId": "order-123", "userId": "user-1"}

    await notification_service.handle_event(event)

    notifications = notification_service.get_recent_notifications()
    assert len(notifications) == 1
    assert "order-123" in notifications[0].body


@pytest.mark.asyncio
async def test_handle_unknown_event(notification_service):
    event = {"event": "UNKNOWN_EVENT", "data": "test"}

    await notification_service.handle_event(event)

    notifications = notification_service.get_recent_notifications()
    assert len(notifications) == 0


@pytest.mark.asyncio
async def test_stats_tracking(notification_service):
    events = [
        {"event": "USER_CREATED", "userId": "u1", "email": "a@b.com"},
        {"event": "ORDER_CREATED", "orderId": "o1", "userId": "u1"},
        {"event": "UNKNOWN_EVENT"},
    ]

    for event in events:
        await notification_service.handle_event(event)

    stats = notification_service.get_stats()
    assert stats["total_received"] == 3
    assert stats["total_sent"] == 2  # UNKNOWN doesn't generate notification


@pytest.mark.asyncio
async def test_notification_limit(notification_service):
    for i in range(10):
        await notification_service.handle_event(
            {"event": "USER_CREATED", "userId": f"u{i}", "email": f"u{i}@test.com"}
        )

    notifications = notification_service.get_recent_notifications(limit=5)
    assert len(notifications) == 5
