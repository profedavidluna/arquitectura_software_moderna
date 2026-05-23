"""
Tests for the CreateProduct use case in Clean Architecture.

These tests demonstrate testing a use case in isolation using
the in-memory gateway. The use case doesn't know or care that
we're using an in-memory implementation.
"""

import pytest
import pytest_asyncio
from decimal import Decimal

from app.usecases.create_product import CreateProductUseCase
from app.usecases.dto import CreateProductInput
from app.usecases.errors import DuplicateSkuError
from app.frameworks.persistence.in_memory_product_gateway import InMemoryProductGateway


@pytest_asyncio.fixture
async def gateway():
    """Create an in-memory gateway for testing."""
    return InMemoryProductGateway()


@pytest_asyncio.fixture
async def use_case(gateway):
    """Create the use case with the in-memory gateway."""
    return CreateProductUseCase(gateway)


@pytest.mark.asyncio
async def test_create_product_success(use_case):
    """Test successful product creation."""
    input_data = CreateProductInput(
        name="Test Product",
        description="A test product",
        price=Decimal("29.99"),
        category="Testing",
        stock_quantity=100,
        sku="TEST-001",
    )

    output = await use_case.execute(input_data)

    assert output.name == "Test Product"
    assert output.price == Decimal("29.99")
    assert output.stock_quantity == 100
    assert output.sku == "TEST-001"
    assert output.active is True
    assert output.id is not None


@pytest.mark.asyncio
async def test_create_product_duplicate_sku(use_case):
    """Test that duplicate SKU raises DuplicateSkuError."""
    input_data = CreateProductInput(
        name="Product 1",
        description="",
        price=Decimal("10.00"),
        category="",
        stock_quantity=5,
        sku="DUP-001",
    )

    await use_case.execute(input_data)

    # Try to create another with same SKU
    duplicate = CreateProductInput(
        name="Product 2",
        description="",
        price=Decimal("20.00"),
        category="",
        stock_quantity=3,
        sku="DUP-001",
    )

    with pytest.raises(DuplicateSkuError):
        await use_case.execute(duplicate)


@pytest.mark.asyncio
async def test_create_product_invalid_price(use_case):
    """Test that invalid price raises ValueError (from entity)."""
    input_data = CreateProductInput(
        name="Bad Product",
        description="",
        price=Decimal("0"),
        category="",
        stock_quantity=0,
        sku="BAD-001",
    )

    with pytest.raises(ValueError, match="Price must be greater than 0"):
        await use_case.execute(input_data)


@pytest.mark.asyncio
async def test_create_product_negative_stock(use_case):
    """Test that negative stock raises ValueError (from entity)."""
    input_data = CreateProductInput(
        name="Bad Product",
        description="",
        price=Decimal("10.00"),
        category="",
        stock_quantity=-5,
        sku="BAD-002",
    )

    with pytest.raises(ValueError, match="Stock quantity cannot be negative"):
        await use_case.execute(input_data)


@pytest.mark.asyncio
async def test_create_product_generates_id(use_case):
    """Test that a UUID is generated for the product."""
    input_data = CreateProductInput(
        name="ID Test",
        description="",
        price=Decimal("10.00"),
        category="",
        stock_quantity=1,
        sku="ID-001",
    )

    output = await use_case.execute(input_data)
    assert output.id is not None
    assert len(output.id) == 36  # UUID format: 8-4-4-4-12
