"""
Tests for the Product entity in Clean Architecture.

Entity tests verify that business rules are correctly enforced
at the innermost layer, independent of any framework or use case.
"""

import pytest
from decimal import Decimal

from app.entities.product import Product


class TestProductCreation:
    """Test the Product.create() factory method."""

    def test_create_valid_product(self):
        """Test creating a product with valid data."""
        product = Product.create(
            name="Test Product",
            description="A test product",
            price=Decimal("29.99"),
            category="Testing",
            stock_quantity=100,
            sku="TEST-001",
        )

        assert product.name == "Test Product"
        assert product.price == Decimal("29.99")
        assert product.stock_quantity == 100
        assert product.sku == "TEST-001"
        assert product.active is True
        assert product.id is not None

    def test_create_product_invalid_price(self):
        """Business rule: price must be > 0."""
        with pytest.raises(ValueError, match="Price must be greater than 0"):
            Product.create(
                name="Bad",
                description="",
                price=Decimal("0"),
                category="",
                stock_quantity=0,
                sku="BAD-001",
            )

    def test_create_product_negative_price(self):
        """Business rule: price must be > 0."""
        with pytest.raises(ValueError, match="Price must be greater than 0"):
            Product.create(
                name="Bad",
                description="",
                price=Decimal("-10"),
                category="",
                stock_quantity=0,
                sku="BAD-002",
            )

    def test_create_product_negative_stock(self):
        """Business rule: stock cannot be negative."""
        with pytest.raises(ValueError, match="Stock quantity cannot be negative"):
            Product.create(
                name="Bad",
                description="",
                price=Decimal("10"),
                category="",
                stock_quantity=-1,
                sku="BAD-003",
            )

    def test_create_product_empty_name(self):
        """Business rule: name is required."""
        with pytest.raises(ValueError, match="Name is required"):
            Product.create(
                name="",
                description="",
                price=Decimal("10"),
                category="",
                stock_quantity=0,
                sku="BAD-004",
            )

    def test_create_product_empty_sku(self):
        """Business rule: SKU is required."""
        with pytest.raises(ValueError, match="SKU is required"):
            Product.create(
                name="Valid Name",
                description="",
                price=Decimal("10"),
                category="",
                stock_quantity=0,
                sku="",
            )


class TestProductStockOperations:
    """Test stock-related business rules."""

    def test_decrease_stock_success(self):
        """Test decreasing stock within available quantity."""
        product = Product.create(
            name="Stockable",
            description="",
            price=Decimal("10"),
            category="",
            stock_quantity=50,
            sku="STOCK-001",
        )

        updated = product.decrease_stock(20)
        assert updated.stock_quantity == 30
        # Original is unchanged (immutability)
        assert product.stock_quantity == 50

    def test_decrease_stock_insufficient(self):
        """Business rule: stock cannot go negative."""
        product = Product.create(
            name="Low Stock",
            description="",
            price=Decimal("10"),
            category="",
            stock_quantity=5,
            sku="LOW-001",
        )

        with pytest.raises(ValueError, match="Insufficient stock"):
            product.decrease_stock(10)

    def test_decrease_stock_zero_quantity(self):
        """Business rule: decrease quantity must be positive."""
        product = Product.create(
            name="Test",
            description="",
            price=Decimal("10"),
            category="",
            stock_quantity=5,
            sku="ZERO-001",
        )

        with pytest.raises(ValueError, match="greater than 0"):
            product.decrease_stock(0)

    def test_increase_stock_success(self):
        """Test increasing stock."""
        product = Product.create(
            name="Restockable",
            description="",
            price=Decimal("10"),
            category="",
            stock_quantity=10,
            sku="RESTOCK-001",
        )

        updated = product.increase_stock(25)
        assert updated.stock_quantity == 35

    def test_increase_stock_zero_quantity(self):
        """Business rule: increase quantity must be positive."""
        product = Product.create(
            name="Test",
            description="",
            price=Decimal("10"),
            category="",
            stock_quantity=5,
            sku="INC-001",
        )

        with pytest.raises(ValueError, match="greater than 0"):
            product.increase_stock(0)


class TestProductDeactivation:
    """Test soft-delete behavior."""

    def test_deactivate(self):
        """Test that deactivate sets active=False."""
        product = Product.create(
            name="Active Product",
            description="",
            price=Decimal("10"),
            category="",
            stock_quantity=1,
            sku="DEACT-001",
        )

        deactivated = product.deactivate()
        assert deactivated.active is False
        # Original unchanged
        assert product.active is True
