"""
Order Service Unit Tests
==========================
Tests the OrderServiceImpl with mocked dependencies.
Focuses on saga state transitions and event publishing.
"""

import pytest
from decimal import Decimal
from unittest.mock import AsyncMock, patch
from uuid import uuid4

from app.application.order_service import OrderServiceImpl
from app.domain.model import Order, OrderItem, OrderStatus


@pytest.fixture
def mock_repository():
    """Create a mock repository."""
    return AsyncMock()


@pytest.fixture
def mock_publisher():
    """Create a mock Kafka publisher."""
    return AsyncMock()


@pytest.fixture
def service(mock_repository, mock_publisher):
    """Create an OrderServiceImpl with mocked dependencies."""
    return OrderServiceImpl(mock_repository, mock_publisher)


@pytest.fixture
def sample_order():
    """Create a sample order for testing."""
    order_id = uuid4()
    return Order(
        id=order_id,
        user_id=uuid4(),
        status=OrderStatus.PENDING,
        total_amount=Decimal("59.98"),
        items=[
            OrderItem(
                id=uuid4(),
                order_id=order_id,
                product_id=uuid4(),
                product_name="Test Product",
                quantity=2,
                unit_price=Decimal("29.99"),
                subtotal=Decimal("59.98"),
            )
        ],
    )


@pytest.mark.asyncio
async def test_create_order_success(service, mock_repository, mock_publisher):
    """Test successful order creation initiates saga."""
    # Arrange
    user_id = uuid4()
    product_id = uuid4()
    items = [
        {
            "product_id": str(product_id),
            "product_name": "Widget",
            "quantity": 3,
            "unit_price": 10.00,
        }
    ]

    # Act
    order = await service.create_order(user_id=user_id, items=items)

    # Assert
    assert order.user_id == user_id
    assert order.status == OrderStatus.PENDING
    assert order.total_amount == Decimal("30.00")
    assert len(order.items) == 1
    assert order.items[0].quantity == 3

    # Verify saga initiation
    mock_repository.save.assert_called_once()
    mock_publisher.publish.assert_called_once()
    call_args = mock_publisher.publish.call_args
    assert call_args[0][0] == "order.created"


@pytest.mark.asyncio
async def test_confirm_order_success(service, mock_repository, mock_publisher, sample_order):
    """Test order confirmation (saga happy path)."""
    # Arrange
    mock_repository.find_by_id.return_value = sample_order

    # Act
    await service.confirm_order(sample_order.id)

    # Assert
    mock_repository.update_status.assert_called_once_with(
        sample_order.id, OrderStatus.CONFIRMED
    )
    # Verify order.confirmed event published
    mock_publisher.publish.assert_called_once()
    call_args = mock_publisher.publish.call_args
    assert call_args[0][0] == "order.confirmed"


@pytest.mark.asyncio
async def test_cancel_order_pending(service, mock_repository, mock_publisher, sample_order):
    """Test cancelling a pending order (no compensation needed)."""
    # Arrange
    mock_repository.find_by_id.return_value = sample_order

    # Act
    await service.cancel_order(sample_order.id, "Insufficient stock")

    # Assert
    mock_repository.update_status.assert_called_once_with(
        sample_order.id, OrderStatus.CANCELLED
    )
    # No order.cancelled event for pending orders (stock wasn't reserved)
    mock_publisher.publish.assert_not_called()


@pytest.mark.asyncio
async def test_cancel_confirmed_order_triggers_compensation(
    service, mock_repository, mock_publisher, sample_order
):
    """Test cancelling a confirmed order publishes compensation event."""
    # Arrange - order is already confirmed
    sample_order.status = OrderStatus.CONFIRMED
    mock_repository.find_by_id.return_value = sample_order

    # Act
    await service.cancel_order(sample_order.id, "User requested")

    # Assert
    mock_repository.update_status.assert_called_once_with(
        sample_order.id, OrderStatus.CANCELLED
    )
    # Compensation: order.cancelled event triggers stock release
    mock_publisher.publish.assert_called_once()
    call_args = mock_publisher.publish.call_args
    assert call_args[0][0] == "order.cancelled"


@pytest.mark.asyncio
async def test_confirm_order_not_found(service, mock_repository, mock_publisher):
    """Test confirming a non-existent order."""
    # Arrange
    mock_repository.find_by_id.return_value = None

    # Act
    await service.confirm_order(uuid4())

    # Assert - no state change or event
    mock_repository.update_status.assert_not_called()
    mock_publisher.publish.assert_not_called()


@pytest.mark.asyncio
async def test_get_by_id(service, mock_repository, sample_order):
    """Test retrieving an order by ID."""
    # Arrange
    mock_repository.find_by_id.return_value = sample_order

    # Act
    result = await service.get_by_id(sample_order.id)

    # Assert
    assert result == sample_order


@pytest.mark.asyncio
async def test_create_order_multiple_items(service, mock_repository, mock_publisher):
    """Test order creation with multiple items calculates total correctly."""
    # Arrange
    items = [
        {"product_id": str(uuid4()), "product_name": "A", "quantity": 2, "unit_price": 10.00},
        {"product_id": str(uuid4()), "product_name": "B", "quantity": 1, "unit_price": 25.50},
    ]

    # Act
    order = await service.create_order(user_id=uuid4(), items=items)

    # Assert
    assert order.total_amount == Decimal("45.50")
    assert len(order.items) == 2
