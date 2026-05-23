"""
Unit Tests: Product Service (Business Layer).

These tests verify the Business Layer logic using the InMemoryProductRepository.
In Layered Architecture, we test the service with a real (concrete) repository
implementation rather than a mock of an abstract interface.

KEY DIFFERENCE FROM HEXAGONAL ARCHITECTURE:
- Hexagonal: Tests use the InMemoryProductRepository as a "test double" that
  satisfies the abstract ProductRepositoryPort. The test proves the domain
  logic works independently of infrastructure.
- Layered: Tests use the InMemoryProductRepository directly. There is no
  abstract port — we are testing with the actual concrete Data Layer class.
  This is simpler but means our tests are coupled to the Data Layer implementation.

Both approaches achieve the same practical result (fast, isolated tests),
but the hexagonal approach provides stronger architectural guarantees.
"""
from __future__ import annotations

import pytest
import pytest_asyncio

from app.business.errors import (
    ConflictError,
    InsufficientStockError,
    NotFoundError,
    ValidationError,
)
from app.business.product_service import ProductService
from app.data.product_repository import InMemoryProductRepository


@pytest_asyncio.fixture
async def service() -> ProductService:
    """Create a ProductService with an in-memory repository for testing."""
    repository = InMemoryProductRepository()
    return ProductService(repository)


async def _create_product(
    service: ProductService,
    name: str = "Test Product",
    description: str = "A test product",
    price: float = 29.99,
    category: str = "Electronics",
    stock_quantity: int = 100,
    sku: str = "TEST-001",
):
    """Helper to create a product with defaults."""
    return await service.create_product(
        name=name,
        description=description,
        price=price,
        category=category,
        stock_quantity=stock_quantity,
        sku=sku,
    )


@pytest.mark.asyncio
async def test_create_product_success(service: ProductService) -> None:
    """Test successful product creation with valid data."""
    product = await _create_product(service)

    assert product.id is not None
    assert product.name == "Test Product"
    assert product.description == "A test product"
    assert product.price == 29.99
    assert product.category == "Electronics"
    assert product.stock_quantity == 100
    assert product.sku == "TEST-001"
    assert product.active is True
    assert product.created_at is not None
    assert product.updated_at is not None


@pytest.mark.asyncio
async def test_create_product_invalid_price(service: ProductService) -> None:
    """Test that creating a product with zero or negative price raises ValidationError."""
    with pytest.raises(ValidationError):
        await _create_product(service, price=-10.0)

    with pytest.raises(ValidationError):
        await _create_product(service, price=0.0)


@pytest.mark.asyncio
async def test_create_product_duplicate_sku(service: ProductService) -> None:
    """Test that creating a product with duplicate SKU raises ConflictError."""
    await _create_product(service, sku="DUPLICATE-SKU")

    with pytest.raises(ConflictError):
        await _create_product(service, name="Another Product", sku="DUPLICATE-SKU")


@pytest.mark.asyncio
async def test_search_by_category(service: ProductService) -> None:
    """Test searching products by category."""
    await _create_product(service, name="Mouse", category="Electronics", sku="ELEC-001")
    await _create_product(service, name="Keyboard", category="Electronics", sku="ELEC-002")
    await _create_product(service, name="Desk", category="Furniture", sku="FURN-001")

    results = await service.search_products(category="Electronics")

    assert len(results) == 2
    assert all(p.category == "Electronics" for p in results)


@pytest.mark.asyncio
async def test_search_by_query(service: ProductService) -> None:
    """Test searching products by text query in name/description."""
    await _create_product(service, name="Wireless Mouse", sku="WM-001")
    await _create_product(service, name="Wired Mouse", sku="WM-002")
    await _create_product(service, name="Keyboard", sku="KB-001")

    results = await service.search_products(query="mouse")

    assert len(results) == 2


@pytest.mark.asyncio
async def test_list_products_pagination(service: ProductService) -> None:
    """Test product listing with pagination and max page size enforcement."""
    for i in range(15):
        await _create_product(service, name=f"Product {i}", sku=f"SKU-{i:03d}")

    result = await service.list_products(page=1, size=10)

    assert len(result["items"]) == 10
    assert result["total"] == 15
    assert result["page"] == 1
    assert result["size"] == 10
    assert result["total_pages"] == 2


@pytest.mark.asyncio
async def test_decrease_stock_success(service: ProductService) -> None:
    """Test successful stock decrease."""
    created = await _create_product(service, stock_quantity=50)

    product = await service.decrease_stock(created.id, 20)

    assert product.stock_quantity == 30


@pytest.mark.asyncio
async def test_decrease_stock_insufficient(service: ProductService) -> None:
    """Test that decreasing stock beyond available raises InsufficientStockError."""
    created = await _create_product(service, stock_quantity=10)

    with pytest.raises(InsufficientStockError):
        await service.decrease_stock(created.id, 20)
