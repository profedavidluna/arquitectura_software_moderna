"""
Unit Tests: Product Service.

These tests verify the domain service behavior using the InMemoryProductRepository.
This demonstrates a key benefit of Hexagonal Architecture: the domain logic can be
tested in complete isolation from infrastructure (no database, no HTTP server).

The InMemoryProductRepository acts as a test double that satisfies the
ProductRepositoryPort contract, allowing us to test business rules without
any external dependencies.
"""
from __future__ import annotations

import pytest
import pytest_asyncio

from app.adapter.output.persistence.in_memory_product_repository import (
    InMemoryProductRepository,
)
from app.domain.model.product import (
    DuplicateSkuError,
    InsufficientStockError,
    InvalidPriceError,
    ProductNotFoundError,
)
from app.domain.port.input.product_service_port import (
    CreateProductCommand,
    SearchCriteria,
)
from app.domain.service.product_service import ProductService


@pytest_asyncio.fixture
async def service() -> ProductService:
    """Create a ProductService with an in-memory repository for testing."""
    repository = InMemoryProductRepository()
    return ProductService(repository)


def _create_command(
    name: str = "Test Product",
    description: str = "A test product",
    price: float = 29.99,
    category: str = "Electronics",
    stock_quantity: int = 100,
    sku: str = "TEST-001",
) -> CreateProductCommand:
    """Helper to create a CreateProductCommand with defaults."""
    return CreateProductCommand(
        name=name,
        description=description,
        price=price,
        category=category,
        stock_quantity=stock_quantity,
        sku=sku,
    )


@pytest.mark.asyncio
async def test_create_product_success(service: ProductService) -> None:
    """Test successful product creation."""
    command = _create_command()

    product = await service.create_product(command)

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
    """Test that creating a product with invalid price raises an error."""
    command = _create_command(price=-10.0)

    with pytest.raises(InvalidPriceError):
        await service.create_product(command)


@pytest.mark.asyncio
async def test_create_product_zero_price(service: ProductService) -> None:
    """Test that creating a product with zero price raises an error."""
    command = _create_command(price=0.0)

    with pytest.raises(InvalidPriceError):
        await service.create_product(command)


@pytest.mark.asyncio
async def test_create_product_duplicate_sku(service: ProductService) -> None:
    """Test that creating a product with duplicate SKU raises an error."""
    command = _create_command(sku="DUPLICATE-SKU")
    await service.create_product(command)

    duplicate_command = _create_command(name="Another Product", sku="DUPLICATE-SKU")

    with pytest.raises(DuplicateSkuError):
        await service.create_product(duplicate_command)


@pytest.mark.asyncio
async def test_get_product_not_found(service: ProductService) -> None:
    """Test that getting a non-existent product raises an error."""
    with pytest.raises(ProductNotFoundError):
        await service.get_product_by_id("non-existent-id")


@pytest.mark.asyncio
async def test_get_product_success(service: ProductService) -> None:
    """Test successful product retrieval."""
    command = _create_command()
    created = await service.create_product(command)

    product = await service.get_product_by_id(created.id)

    assert product.id == created.id
    assert product.name == "Test Product"


@pytest.mark.asyncio
async def test_decrease_stock_success(service: ProductService) -> None:
    """Test successful stock decrease."""
    command = _create_command(stock_quantity=50)
    created = await service.create_product(command)

    product = await service.decrease_stock(created.id, 20)

    assert product.stock_quantity == 30


@pytest.mark.asyncio
async def test_decrease_stock_insufficient(service: ProductService) -> None:
    """Test that decreasing stock beyond available raises an error."""
    command = _create_command(stock_quantity=10)
    created = await service.create_product(command)

    with pytest.raises(InsufficientStockError):
        await service.decrease_stock(created.id, 20)


@pytest.mark.asyncio
async def test_increase_stock_success(service: ProductService) -> None:
    """Test successful stock increase."""
    command = _create_command(stock_quantity=50)
    created = await service.create_product(command)

    product = await service.increase_stock(created.id, 30)

    assert product.stock_quantity == 80


@pytest.mark.asyncio
async def test_search_by_category(service: ProductService) -> None:
    """Test searching products by category."""
    await service.create_product(
        _create_command(name="Mouse", category="Electronics", sku="ELEC-001")
    )
    await service.create_product(
        _create_command(name="Keyboard", category="Electronics", sku="ELEC-002")
    )
    await service.create_product(
        _create_command(name="Desk", category="Furniture", sku="FURN-001")
    )

    criteria = SearchCriteria(category="Electronics")
    results = await service.search_products(criteria)

    assert len(results) == 2
    assert all(p.category == "Electronics" for p in results)


@pytest.mark.asyncio
async def test_search_by_query(service: ProductService) -> None:
    """Test searching products by text query."""
    await service.create_product(
        _create_command(name="Wireless Mouse", sku="WM-001")
    )
    await service.create_product(
        _create_command(name="Wired Mouse", sku="WM-002")
    )
    await service.create_product(
        _create_command(name="Keyboard", sku="KB-001")
    )

    criteria = SearchCriteria(query="mouse")
    results = await service.search_products(criteria)

    assert len(results) == 2


@pytest.mark.asyncio
async def test_list_products_pagination(service: ProductService) -> None:
    """Test product listing with pagination."""
    for i in range(15):
        await service.create_product(
            _create_command(name=f"Product {i}", sku=f"SKU-{i:03d}")
        )

    result = await service.list_products(page=1, size=10)

    assert len(result.items) == 10
    assert result.total == 15
    assert result.page == 1
    assert result.size == 10
    assert result.total_pages == 2


@pytest.mark.asyncio
async def test_delete_product_success(service: ProductService) -> None:
    """Test successful product deletion."""
    command = _create_command()
    created = await service.create_product(command)

    await service.delete_product(created.id)

    with pytest.raises(ProductNotFoundError):
        await service.get_product_by_id(created.id)


@pytest.mark.asyncio
async def test_delete_product_not_found(service: ProductService) -> None:
    """Test that deleting a non-existent product raises an error."""
    with pytest.raises(ProductNotFoundError):
        await service.delete_product("non-existent-id")
