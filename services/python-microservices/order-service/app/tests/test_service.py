import pytest
from unittest.mock import AsyncMock, MagicMock
from decimal import Decimal
from app.application.service import OrderService
from app.application.saga import SagaOrchestrator, SagaResult
from app.domain.models import Order, OrderItem


@pytest.fixture
def mock_repository():
    return AsyncMock()


@pytest.fixture
def mock_producer():
    return AsyncMock()


@pytest.fixture
def mock_saga():
    return AsyncMock(spec=SagaOrchestrator)


@pytest.fixture
def order_service(mock_repository, mock_producer, mock_saga):
    return OrderService(mock_repository, mock_producer, mock_saga)


@pytest.mark.asyncio
async def test_create_order_success(order_service, mock_repository, mock_saga, mock_producer):
    mock_repository.create.return_value = Order(
        id="order-1", user_id="user-1", total_amount=Decimal("100.00")
    )
    mock_saga.execute.return_value = [
        SagaResult(success=True, step="CART_CHECKOUT"),
        SagaResult(success=True, step="INVENTORY_RESERVE"),
        SagaResult(success=True, step="PAYMENT_PROCESS"),
        SagaResult(success=True, step="CONFIRM"),
    ]
    mock_repository.find_by_id.return_value = Order(
        id="order-1", user_id="user-1", status="CONFIRMED",
        total_amount=Decimal("100.00"), saga_status="COMPLETED"
    )

    result = await order_service.create_order(
        user_id="user-1", cart_id="cart-1",
        items=[{"productId": "p1", "productName": "Item", "price": "50.00", "quantity": 2}],
        total_amount=Decimal("100.00"),
    )

    assert result.status == "CONFIRMED"
    mock_repository.update_status.assert_called_with("order-1", "CONFIRMED")


@pytest.mark.asyncio
async def test_create_order_saga_failure(order_service, mock_repository, mock_saga):
    mock_repository.create.return_value = Order(
        id="order-2", user_id="user-1", total_amount=Decimal("50.00")
    )
    mock_saga.execute.return_value = [
        SagaResult(success=True, step="CART_CHECKOUT"),
        SagaResult(success=False, step="INVENTORY_RESERVE", error="Out of stock"),
    ]
    mock_repository.find_by_id.return_value = Order(
        id="order-2", user_id="user-1", status="FAILED",
        total_amount=Decimal("50.00"), saga_status="COMPENSATED"
    )

    result = await order_service.create_order(
        user_id="user-1", cart_id="cart-1",
        items=[{"productId": "p1", "productName": "Item", "price": "50.00", "quantity": 1}],
        total_amount=Decimal("50.00"),
    )

    assert result.status == "FAILED"
    mock_repository.update_saga_status.assert_called_with("order-2", "COMPENSATED")


@pytest.mark.asyncio
async def test_cancel_order_success(order_service, mock_repository, mock_producer):
    mock_repository.find_by_id.return_value = Order(
        id="order-1", user_id="user-1", status="CONFIRMED",
        total_amount=Decimal("100.00")
    )

    # After cancel
    async def find_after_cancel(order_id):
        return Order(id="order-1", user_id="user-1", status="CANCELLED",
                     total_amount=Decimal("100.00"))

    mock_repository.find_by_id.side_effect = [
        Order(id="order-1", user_id="user-1", status="CONFIRMED", total_amount=Decimal("100.00")),
        Order(id="order-1", user_id="user-1", status="CANCELLED", total_amount=Decimal("100.00")),
    ]

    result = await order_service.cancel_order("order-1")

    assert result.status == "CANCELLED"


@pytest.mark.asyncio
async def test_cancel_order_invalid_status(order_service, mock_repository):
    mock_repository.find_by_id.return_value = Order(
        id="order-1", user_id="user-1", status="SHIPPED",
        total_amount=Decimal("100.00")
    )

    with pytest.raises(ValueError, match="Cannot cancel"):
        await order_service.cancel_order("order-1")
