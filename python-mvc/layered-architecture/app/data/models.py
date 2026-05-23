"""
Data Layer: Product Model.

In Layered Architecture, the Data Layer is the lowest layer responsible for
data persistence and retrieval. Models here represent the data structures
used for storage.

KEY DIFFERENCE FROM HEXAGONAL ARCHITECTURE:
- Hexagonal: Domain entities are immutable, framework-independent, and contain
  business rules. They live in the domain core.
- Layered: Data models are mutable dataclasses used directly across layers.
  There is no strict separation between "domain entity" and "persistence model".
  The same model flows from Data → Business → Presentation layers.

This coupling is simpler but means changes to the data model affect all layers.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional
from uuid import uuid4


@dataclass
class Product:
    """
    Product data model.

    Mutable dataclass representing a product in the catalog.
    Unlike hexagonal architecture where the entity is immutable and contains
    business rules, this is a simple data container that flows through all layers.

    Mutability makes it simpler to work with but less safe for concurrent access.
    """

    id: str = field(default_factory=lambda: str(uuid4()))
    name: str = ""
    description: str = ""
    price: float = 0.0
    category: str = ""
    stock_quantity: int = 0
    sku: str = ""
    active: bool = True
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None
