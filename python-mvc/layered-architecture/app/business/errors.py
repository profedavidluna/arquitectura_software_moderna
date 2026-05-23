"""
Business Layer: Custom Error Classes.

In Layered Architecture, business errors are defined in the Business Layer
and propagated upward to the Presentation Layer, which maps them to HTTP codes.

KEY DIFFERENCE FROM HEXAGONAL ARCHITECTURE:
- Hexagonal: Domain exceptions live in the domain core and are part of the
  ubiquitous language. They are completely independent of any framework.
- Layered: Business errors serve the same purpose but are defined in the
  Business Layer rather than a protected domain core. The Presentation Layer
  imports directly from the Business Layer (no port/adapter indirection).

The coupling direction is: Presentation → Business → Data (top-down only).
"""
from __future__ import annotations


class NotFoundError(Exception):
    """Raised when a requested resource cannot be found."""

    def __init__(self, message: str = "Resource not found") -> None:
        self.message = message
        super().__init__(self.message)


class ValidationError(Exception):
    """Raised when input data fails business validation rules."""

    def __init__(self, message: str = "Validation failed") -> None:
        self.message = message
        super().__init__(self.message)


class ConflictError(Exception):
    """Raised when an operation conflicts with existing data (e.g., duplicate SKU)."""

    def __init__(self, message: str = "Resource conflict") -> None:
        self.message = message
        super().__init__(self.message)


class InsufficientStockError(Exception):
    """Raised when there is not enough stock to fulfill an operation."""

    def __init__(self, message: str = "Insufficient stock") -> None:
        self.message = message
        super().__init__(self.message)
