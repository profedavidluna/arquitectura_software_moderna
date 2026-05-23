"""
Product Service Unit Tests
============================
Tests the ProductServiceImpl with mocked dependencies.

Testing Strategy:
- Mock the repository (no real database needed)
- Mock the Kafka publisher (no real broker needed)
- Test business logic in isolation
- Verify correct interactions with dependencies

This follows the SOA testing principle:
services should be testable in isolation without infrastructure.
"""

import pytest
from decimal import Decimal
from unittest.mock import AsyncMock, MagicMock, patch
from uuid import uuid4

from app.application.product_service import ProductServiceImpl
from app.domain.model import Product


@pytest.fixture
def mock_repository():
    """Create a mock repository with async methods."""
    repo = AsyncMock()
    return repo


@pytest.fixture
def mock_publisher():
    """Create a mock Kafka publisher with async methods."""
    publisher = AsyncMock()
    return publisher


@pytest.fixture
def service(mock_repository, mock_publisher):
    """Create a ProductServiceImpl with mocked dependencies."""
    return ProductServiceImpl(mock_repository, mock_publisher)


@pytest.mark.asyncio
async def test_create_product_success(service, mock_repository, mock_publisher):
    """Test successful product creation."""
    # Act
    product = await service.create_product(
        name="Test Product",
        description="A test product",
        price=29.99,
        category="Electronics",
        sku="TEST-001",
    )

    # Assert
    assert product.name == "Test Product"
    assert product.description == "A test product"
    assert product.price == Decimal("29.99")
    assert product.category == "Electronics"
    assert product.sku == "TEST-001"
    assert product.active is True

    # Verify repository was called
    mock_repository.save.assert_called_once_with(product)

    # Verify event was published
    mock_publisher.publish.assert_called_once()
    call_args = mock_publisher.publish.call_args
    assert call_args[0][0] == "product.created"  # topic
    assert call_args[0][1] == str(product.id)  # key


@pytest.mark.asyncio
async def test_get_by_id_found(service, mock_repository):
    """Test retrieving an existing product."""
    # Arrange
    product_id = uuid4()
    expected_product = Product(
        id=product_id,
        name="Found Product",
        description="Exists",
        price=Decimal("10.00"),
        category="Books",
        sku="BOOK-001",
    )
    mock_repository.find_by_id.return_value = expected_product

    # Act
    result = await service.get_by_id(product_id)

    # Assert
    assert result == expected_product
    mock_repository.find_by_id.assert_called_once_with(product_id)


@pytest.mark.asyncio
async def test_get_by_id_not_found(service, mock_repository):
    """Test retrieving a non-existent product returns None."""
    # Arrange
    mock_repository.find_by_id.return_value = None

    # Act
    result = await service.get_by_id(uuid4())

    # Assert
    assert result is None


@pytest.mark.asyncio
async def test_update_product_success(service, mock_repository):
    """Test successful product update."""
    # Arrange
    product_id = uuid4()
    existing_product = Product(
        id=product_id,
        name="Old Name",
        description="Old desc",
        price=Decimal("10.00"),
        category="Books",
        sku="BOOK-001",
    )
    mock_repository.find_by_id.return_value = existing_product

    # Act
    result = await service.update_product(
        product_id=product_id,
        name="New Name",
        price=15.99,
    )

    # Assert
    assert result is not None
    assert result.name == "New Name"
    assert result.price == Decimal("15.99")
    mock_repository.update.assert_called_once()


@pytest.mark.asyncio
async def test_update_product_not_found(service, mock_repository):
    """Test updating a non-existent product returns None."""
    # Arrange
    mock_repository.find_by_id.return_value = None

    # Act
    result = await service.update_product(product_id=uuid4(), name="New Name")

    # Assert
    assert result is None
    mock_repository.update.assert_not_called()


@pytest.mark.asyncio
async def test_delete_product_success(service, mock_repository):
    """Test successful product deletion (soft delete)."""
    # Arrange
    product_id = uuid4()
    existing_product = Product(
        id=product_id,
        name="To Delete",
        description="Will be deactivated",
        price=Decimal("5.00"),
        category="Misc",
        sku="DEL-001",
    )
    mock_repository.find_by_id.return_value = existing_product

    # Act
    result = await service.delete_product(product_id)

    # Assert
    assert result is True
    assert existing_product.active is False
    mock_repository.update.assert_called_once()


@pytest.mark.asyncio
async def test_delete_product_not_found(service, mock_repository):
    """Test deleting a non-existent product returns False."""
    # Arrange
    mock_repository.find_by_id.return_value = None

    # Act
    result = await service.delete_product(uuid4())

    # Assert
    assert result is False


@pytest.mark.asyncio
async def test_get_all_products(service, mock_repository):
    """Test retrieving all products."""
    # Arrange
    products = [
        Product(id=uuid4(), name="P1", description="", price=Decimal("10"), category="A", sku="S1"),
        Product(id=uuid4(), name="P2", description="", price=Decimal("20"), category="B", sku="S2"),
    ]
    mock_repository.find_all.return_value = products

    # Act
    result = await service.get_all()

    # Assert
    assert len(result) == 2
    assert result[0].name == "P1"
    assert result[1].name == "P2"
