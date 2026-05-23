"""
@layer Entities (Innermost Layer)
@description Enterprise business rules - the Product domain entity.

In Clean Architecture, entities encapsulate the most general and high-level
business rules. They are the least likely to change when something external
changes (e.g., page navigation, security, or database).

Entities:
- Have NO dependencies on any outer layer
- Contain enterprise-wide business rules
- Can be used by many different applications
- Are plain Python objects with business logic

This is the CORE of the application - everything else depends on this,
but this depends on nothing.
"""

from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal
from uuid import uuid4


@dataclass
class Product:
    """
    Product entity with enterprise business rules.

    This entity knows nothing about databases, HTTP, or frameworks.
    It only contains business rules that would be true regardless
    of how the application is delivered.
    """
    id: str
    name: str
    description: str
    price: Decimal
    category: str
    stock_quantity: int
    sku: str
    active: bool = True
    created_at: datetime = field(default_factory=datetime.utcnow)
    updated_at: datetime = field(default_factory=datetime.utcnow)

    @staticmethod
    def create(
        name: str,
        description: str,
        price: Decimal,
        category: str,
        stock_quantity: int,
        sku: str,
    ) -> "Product":
        """
        Factory method - creates a new Product with validation.

        This is the ONLY way to create a valid Product from scratch.
        It enforces all business rules at creation time.
        """
        if price <= 0:
            raise ValueError("Price must be greater than 0")
        if stock_quantity < 0:
            raise ValueError("Stock quantity cannot be negative")
        if not name or not name.strip():
            raise ValueError("Name is required")
        if not sku or not sku.strip():
            raise ValueError("SKU is required")

        return Product(
            id=str(uuid4()),
            name=name.strip(),
            description=description,
            price=price,
            category=category,
            stock_quantity=stock_quantity,
            sku=sku.strip(),
            active=True,
            created_at=datetime.utcnow(),
            updated_at=datetime.utcnow(),
        )

    def decrease_stock(self, quantity: int) -> "Product":
        """
        Business rule: stock cannot go negative.
        Returns a new Product instance with decreased stock.
        """
        if quantity <= 0:
            raise ValueError("Quantity to decrease must be greater than 0")
        new_stock = self.stock_quantity - quantity
        if new_stock < 0:
            raise ValueError(
                f"Insufficient stock. Available: {self.stock_quantity}, requested: {quantity}"
            )
        return Product(
            id=self.id,
            name=self.name,
            description=self.description,
            price=self.price,
            category=self.category,
            stock_quantity=new_stock,
            sku=self.sku,
            active=self.active,
            created_at=self.created_at,
            updated_at=datetime.utcnow(),
        )

    def increase_stock(self, quantity: int) -> "Product":
        """Business rule: quantity must be positive."""
        if quantity <= 0:
            raise ValueError("Quantity to increase must be greater than 0")
        return Product(
            id=self.id,
            name=self.name,
            description=self.description,
            price=self.price,
            category=self.category,
            stock_quantity=self.stock_quantity + quantity,
            sku=self.sku,
            active=self.active,
            created_at=self.created_at,
            updated_at=datetime.utcnow(),
        )

    def deactivate(self) -> "Product":
        """Soft delete - sets active to False."""
        return Product(
            id=self.id,
            name=self.name,
            description=self.description,
            price=self.price,
            category=self.category,
            stock_quantity=self.stock_quantity,
            sku=self.sku,
            active=False,
            created_at=self.created_at,
            updated_at=datetime.utcnow(),
        )

    def update(
        self,
        name: str = None,
        description: str = None,
        price: Decimal = None,
        category: str = None,
        stock_quantity: int = None,
        sku: str = None,
    ) -> "Product":
        """Create a new Product with updated fields, validating business rules."""
        new_price = price if price is not None else self.price
        new_stock = stock_quantity if stock_quantity is not None else self.stock_quantity

        if new_price <= 0:
            raise ValueError("Price must be greater than 0")
        if new_stock < 0:
            raise ValueError("Stock quantity cannot be negative")

        return Product(
            id=self.id,
            name=name if name is not None else self.name,
            description=description if description is not None else self.description,
            price=new_price,
            category=category if category is not None else self.category,
            stock_quantity=new_stock,
            sku=sku if sku is not None else self.sku,
            active=self.active,
            created_at=self.created_at,
            updated_at=datetime.utcnow(),
        )
