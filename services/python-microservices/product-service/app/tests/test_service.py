import pytest
from unittest.mock import AsyncMock
from decimal import Decimal
from app.application.service import ProductService
from app.domain.models import Product


@pytest.fixture
def mock_repository():
    return AsyncMock()


@pytest.fixture
def mock_producer():
    return AsyncMock()


@pytest.fixture
def mock_cache():
    return AsyncMock()


@pytest.fixture
def product_service(mock_repository, mock_producer, mock_cache):
    return ProductService(mock_repository, mock_producer, mock_cache)


@pytest.mark.asyncio
async def test_create_product(product_service, mock_repository, mock_producer):
    mock_repository.create.return_value = Product(
        id="prod-1", name="Test Product", price=Decimal("29.99"), status="ACTIVE"
    )

    result = await product_service.create_product(
        name="Test Product", description="A test", price=Decimal("29.99")
    )

    assert result.name == "Test Product"
    assert result.price == Decimal("29.99")
    mock_producer.publish.assert_called_once()


@pytest.mark.asyncio
async def test_get_product_from_cache(product_service, mock_cache, mock_repository):
    import json
    mock_cache.get.return_value = json.dumps({
        "id": "prod-1", "name": "Cached Product", "description": None,
        "price": "19.99", "category_id": None, "image_url": None,
        "status": "ACTIVE", "created_at": "2024-01-01T00:00:00",
        "updated_at": "2024-01-01T00:00:00",
    })

    result = await product_service.get_product("prod-1")

    assert result.name == "Cached Product"
    mock_repository.find_by_id.assert_not_called()


@pytest.mark.asyncio
async def test_get_product_cache_miss(product_service, mock_cache, mock_repository):
    mock_cache.get.return_value = None
    mock_repository.find_by_id.return_value = Product(
        id="prod-1", name="DB Product", price=Decimal("15.00"), status="ACTIVE"
    )

    result = await product_service.get_product("prod-1")

    assert result.name == "DB Product"
    mock_cache.set.assert_called_once()


@pytest.mark.asyncio
async def test_update_product_invalidates_cache(product_service, mock_repository, mock_cache):
    mock_repository.find_by_id.return_value = Product(
        id="prod-1", name="Old Name", price=Decimal("10.00"), status="ACTIVE"
    )
    mock_repository.update.return_value = Product(
        id="prod-1", name="New Name", price=Decimal("10.00"), status="ACTIVE"
    )

    result = await product_service.update_product("prod-1", name="New Name")

    assert result.name == "New Name"
    mock_cache.delete.assert_called_once_with("product:prod-1")


@pytest.mark.asyncio
async def test_search_products(product_service, mock_repository):
    mock_repository.search.return_value = [
        Product(id="p1", name="Python Book", price=Decimal("39.99")),
        Product(id="p2", name="Python Course", price=Decimal("49.99")),
    ]

    results = await product_service.search_products("Python")

    assert len(results) == 2
    mock_repository.search.assert_called_once_with("Python")
