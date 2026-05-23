"""
@layer Use Cases - Errors
@description Application-specific exceptions for use cases.

These exceptions represent application-level errors that can occur
during use case execution. They are different from entity validation
errors (which are business rule violations).
"""


class UseCaseError(Exception):
    """Base class for use case errors."""
    pass


class ProductNotFoundError(UseCaseError):
    """Raised when a product cannot be found."""

    def __init__(self, product_id: str):
        self.product_id = product_id
        super().__init__(f"Product with ID '{product_id}' not found")


class DuplicateSkuError(UseCaseError):
    """Raised when a product with the same SKU already exists."""

    def __init__(self, sku: str):
        self.sku = sku
        super().__init__(f"Product with SKU '{sku}' already exists")
