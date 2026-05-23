import pytest
from unittest.mock import AsyncMock
from app.application.service import InventoryService, InsufficientStockError
from app.domain.models import InventoryItem, Reservation


@pytest.fixture
def mock_repository():
    return AsyncMock()


@pytest.fixture
def mock_producer():
    return AsyncMock()


@pytest.fixture
def inventory_service(mock_repository, mock_producer):
    return InventoryService(mock_repository, mock_producer)


@pytest.mark.asyncio
async def test_add_stock_new_product(inventory_service, mock_repository, mock_producer):
    mock_repository.find_by_product.return_value = None
    mock_repository.create.return_value = InventoryItem(
        id="inv-1", product_id="prod-1", quantity=100
    )

    result = await inventory_service.add_stock("prod-1", 100, "A1")

    assert result.quantity == 100
    mock_repository.create.assert_called_once()
    mock_producer.publish.assert_called_once()


@pytest.mark.asyncio
async def test_add_stock_existing_product(inventory_service, mock_repository):
    mock_repository.find_by_product.return_value = InventoryItem(
        id="inv-1", product_id="prod-1", quantity=50
    )

    result = await inventory_service.add_stock("prod-1", 30)

    assert result.quantity == 80
    mock_repository.update_quantity.assert_called_once_with("prod-1", 80)


@pytest.mark.asyncio
async def test_reserve_items_success(inventory_service, mock_repository, mock_producer):
    mock_repository.find_by_product.return_value = InventoryItem(
        id="inv-1", product_id="prod-1", quantity=100, reserved=10
    )
    mock_repository.reserve_stock.return_value = True
    mock_repository.create_reservation.return_value = Reservation(
        id="res-1", order_id="order-1", product_id="prod-1", quantity=5
    )

    result = await inventory_service.reserve_items(
        "order-1", [{"productId": "prod-1", "quantity": 5}]
    )

    assert len(result) == 1
    assert result[0].quantity == 5
    mock_producer.publish.assert_called_once()


@pytest.mark.asyncio
async def test_reserve_items_insufficient_stock(inventory_service, mock_repository):
    mock_repository.find_by_product.return_value = InventoryItem(
        id="inv-1", product_id="prod-1", quantity=10, reserved=8
    )

    with pytest.raises(InsufficientStockError):
        await inventory_service.reserve_items(
            "order-1", [{"productId": "prod-1", "quantity": 5}]
        )


@pytest.mark.asyncio
async def test_handle_order_cancelled_event(inventory_service, mock_repository, mock_producer):
    mock_repository.find_reservations_by_order.return_value = [
        Reservation(id="res-1", order_id="order-1", product_id="prod-1", quantity=3)
    ]

    await inventory_service.handle_order_event({
        "event": "ORDER_CANCELLED",
        "orderId": "order-1",
        "items": [{"productId": "prod-1", "quantity": 3}],
    })

    mock_repository.release_stock.assert_called_once_with("prod-1", 3)
