"""
Product Domain Model
=====================
Pure domain entity using Python dataclasses.
No framework dependencies - this is the core of the service.

Design Pattern: Domain Model
- Encapsulates business data and identity
- Immutable by default (frozen=False for updates)
- Uses UUID for distributed-friendly identifiers
"""

from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal
from uuid import UUID


@dataclass
class Product:
    """
    Represents a product in the catalog.

    Attributes:
        id: Unique identifier (UUID)
        name: Product display name
        description: Detailed product description
        price: Product price with decimal precision
        category: Product category for classification
        sku: Stock Keeping Unit - unique business identifier
        active: Whether the product is available for sale
        created_at: Timestamp of creation
        updated_at: Timestamp of last modification
    """

    id: UUID
    name: str
    description: str
    price: Decimal
    category: str
    sku: str
    active: bool = True
    created_at: datetime = field(default_factory=datetime.utcnow)
    updated_at: datetime = field(default_factory=datetime.utcnow)

    def deactivate(self) -> None:
        """Mark product as inactive (soft delete)."""
        self.active = False
        self.updated_at = datetime.utcnow()

    def update_price(self, new_price: Decimal) -> None:
        """Update product price with timestamp."""
        self.price = new_price
        self.updated_at = datetime.utcnow()

    def to_dict(self) -> dict:
        """Convert to dictionary for serialization."""
        return {
            "id": str(self.id),
            "name": self.name,
            "description": self.description,
            "price": float(self.price),
            "category": self.category,
            "sku": self.sku,
            "active": self.active,
            "created_at": self.created_at.isoformat(),
            "updated_at": self.updated_at.isoformat(),
        }
