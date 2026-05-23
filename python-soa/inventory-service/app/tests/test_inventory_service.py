"""
Inventory Service Unit Tests
===============================
Tests the InventoryServiceImpl with mocked dependencies.
Focuses on stock reservation/release logic and saga participation.
"""

import pytest
from unittest.mock import AsyncMock
from uuid import uuid4

from app.application.inventory_service import InventoryServiceImpl
from app.domain.model import InventoryItem


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
    """Create an InventoryServiceImpl with mocked dependencies."""
    return InventoryServiceImpl(mock_repository, mock_publisher)


@pytest.fixture
def sample_inventory():
    """Create a sample inventory item with stock."""
    return InventoryItem(
        id=uuid4(),
        product_id=uuid4(),
        product_name="Test Product",
        quantity_available=100,
        quantity_reserved=10,
    )


@pytest.mark.asyncio
async def test_create_inventory(service, mock_repository):
    """Test creating a new inventory entry."""
    # Act
    product_id = uuid4()
    item = await service.create_inventory(
        product_id=product_id,
        product_name="New Product",
        initial_quantity=50,
    )

    # Assert
    assert item.product_id == product_id
    assert item.product_name == "New Product"
    assert item.quantity_available == 50
    assert item.quantity_reserved == 0
    mock_repository.save.assert_called_once()


@pytest.mark.asyncio
async def test_add_stock_success(service, mock_repository, sample_inventory):
    """Test adding stock to an existing item."""
    # Arrange
    mock_repository.find_by_product_id.return_value = sample_inventory
    original_qty = sample_inventory.quantity_available

    # Act
    result = await service.add_stock(sample_inventory.product_id, 25)

    # Assert
    assert result is not None
    assert result.quantity_available == original_qty + 25
    mock_repository.update.assert_called_once()


@pytest.mark.asyncio
async def test_add_stock_not_found(service, mock_repository):
    """Test adding stock to a non-existent item."""
    # Arrange
    mock_repository.find_by_product_id.return_value = None

    # Act
    result = await service.add_stock(uuid4(), 10)

    # Assert
    assert result is None


@pytest.mark.asyncio
async def test_reserve_stock_success(service, mock_repository, mock_publisher):
    """Test successful stock reservation (saga happy path)."""
    # Arrange
    product_id = uuid4()
    inventory = InventoryItem(
        id=uuid4(),
        product_id=product_id,
        product_name="Widget",
        quantity_available=50,
        quantity_reserved=0,
    )
    mock_repository.find_by_product_id.return_value = inventory

    order_id = uuid4()
    items = [{"product_id": str(product_id), "quantity": 5}]

    # Act
    result = await service.reserve_stock(order_id, items)

    # Assert
    assert result is True
    assert inventory.quantity_available == 45
    assert inventory.quantity_reserved == 5
    mock_repository.update.assert_called_once()

    # Verify stock.reserved event published
    mock_publisher.publish.assert_called_once()
    call_args = mock_publisher.publish.call_args
    assert call_args[0][0] == "stock.reserved"


@pytest.mark.asyncio
async def test_reserve_stock_insufficient(service, mock_repository, mock_publisher):
    """Test stock reservation failure (saga compensation trigger)."""
    # Arrange
    product_id = uuid4()
    inventory = InventoryItem(
        id=uuid4(),
        product_id=product_id,
        product_name="Widget",
        quantity_available=3,  # Only 3 available
        quantity_reserved=0,
    )
    mock_repository.find_by_product_id.return_value = inventory

    order_id = uuid4()
    items = [{"product_id": str(product_id), "quantity": 10}]  # Requesting 10

    # Act
    result = await service.reserve_stock(order_id, items)

    # Assert
    assert result is False

    # Verify stock.insufficient event published
    mock_publisher.publish.assert_called_once()
    call_args = mock_publisher.publish.call_args
    assert call_args[0][0] == "stock.insufficient"


@pytest.mark.asyncio
async def test_reserve_stock_product_not_found(service, mock_repository, mock_publisher):
    """Test reservation when product has no inventory entry."""
    # Arrange
    mock_repository.find_by_product_id.return_value = None

    order_id = uuid4()
    items = [{"product_id": str(uuid4()), "quantity": 5}]

    # Act
    result = await service.reserve_stock(order_id, items)

    # Assert
    assert result is False
    mock_publisher.publish.assert_called_once()
    call_args = mock_publisher.publish.call_args
    assert call_args[0][0] == "stock.insufficient"


@pytest.mark.asyncio
async def test_release_stock_success(service, mock_repository, mock_publisher):
    """Test stock release (saga compensation)."""
    # Arrange
    product_id = uuid4()
    inventory = InventoryItem(
        id=uuid4(),
        product_id=product_id,
        product_name="Widget",
        quantity_available=45,
        quantity_reserved=5,
    )
    mock_repository.find_by_product_id.return_value = inventory

    order_id = uuid4()
    items = [{"product_id": str(product_id), "quantity": 5}]

    # Act
    await service.release_stock(order_id, items)

    # Assert
    assert inventory.quantity_available == 50
    assert inventory.quantity_reserved == 0
    mock_repository.update.assert_called_once()

    # Verify stock.released event published
    mock_publisher.publish.assert_called_once()
    call_args = mock_publisher.publish.call_args
    assert call_args[0][0] == "stock.released"


@pytest.mark.asyncio
async def test_reserve_stock_multiple_items_partial_failure(
    service, mock_repository, mock_publisher
):
    """Test that partial reservations are rolled back on failure."""
    # Arrange
    product_id_1 = uuid4()
    product_id_2 = uuid4()

    inventory_1 = InventoryItem(
        id=uuid4(), product_id=product_id_1, product_name="A",
        quantity_available=50, quantity_reserved=0,
    )
    inventory_2 = InventoryItem(
        id=uuid4(), product_id=product_id_2, product_name="B",
        quantity_available=2, quantity_reserved=0,  # Only 2 available
    )

    # First call returns inventory_1, second returns inventory_2
    mock_repository.find_by_product_id.side_effect = [inventory_1, inventory_2]

    order_id = uuid4()
    items = [
        {"product_id": str(product_id_1), "quantity": 5},
        {"product_id": str(product_id_2), "quantity": 10},  # Will fail
    ]

    # Act
    result = await service.reserve_stock(order_id, items)

    # Assert
    assert result is False
    # inventory_1 should be rolled back
    assert inventory_1.quantity_available == 50
    assert inventory_1.quantity_reserved == 0

    # Verify stock.insufficient event published
    mock_publisher.publish.assert_called_once()
    call_args = mock_publisher.publish.call_args
    assert call_args[0][0] == "stock.insufficient"
