"""
Product Router (REST API)
===========================
FastAPI router defining the Product Service HTTP endpoints.

SOA Principle: Standardized Service Contract
- RESTful API with consistent URL patterns
- Standard HTTP methods (GET, POST, PUT, DELETE)
- Consistent response format with proper status codes

Design Pattern: Controller/Router
- Thin layer that delegates to the service
- Handles HTTP concerns (status codes, headers)
- Converts between DTOs and domain objects
"""

import logging
from uuid import UUID

from fastapi import APIRouter, HTTPException, status

from app.application.product_service import ProductServiceImpl
from app.infrastructure.web.dto import (
    CreateProductRequest,
    UpdateProductRequest,
    ProductResponse,
)

logger = logging.getLogger(__name__)

router = APIRouter()

# Service instance injected at startup via lifespan
_service: ProductServiceImpl | None = None


def set_service(service: ProductServiceImpl) -> None:
    """Inject the service instance (called during app startup)."""
    global _service
    _service = service


def _get_service() -> ProductServiceImpl:
    """Get the service instance, raising if not initialized."""
    if _service is None:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Service not initialized",
        )
    return _service


@router.post(
    "",
    response_model=ProductResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new product",
)
async def create_product(request: CreateProductRequest):
    """
    Create a new product in the catalog.

    This endpoint:
    1. Validates the request body
    2. Creates the product in the database
    3. Publishes a 'product.created' event to Kafka
    4. Returns the created product
    """
    service = _get_service()
    product = await service.create_product(
        name=request.name,
        description=request.description,
        price=request.price,
        category=request.category,
        sku=request.sku,
    )
    return ProductResponse(
        id=product.id,
        name=product.name,
        description=product.description,
        price=float(product.price),
        category=product.category,
        sku=product.sku,
        active=product.active,
        created_at=product.created_at,
        updated_at=product.updated_at,
    )


@router.get(
    "",
    response_model=list[ProductResponse],
    summary="List all products",
)
async def list_products():
    """Retrieve all active products from the catalog."""
    service = _get_service()
    products = await service.get_all()
    return [
        ProductResponse(
            id=p.id,
            name=p.name,
            description=p.description,
            price=float(p.price),
            category=p.category,
            sku=p.sku,
            active=p.active,
            created_at=p.created_at,
            updated_at=p.updated_at,
        )
        for p in products
    ]


@router.get(
    "/{product_id}",
    response_model=ProductResponse,
    summary="Get product by ID",
)
async def get_product(product_id: UUID):
    """Retrieve a specific product by its UUID."""
    service = _get_service()
    product = await service.get_by_id(product_id)
    if not product:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Product {product_id} not found",
        )
    return ProductResponse(
        id=product.id,
        name=product.name,
        description=product.description,
        price=float(product.price),
        category=product.category,
        sku=product.sku,
        active=product.active,
        created_at=product.created_at,
        updated_at=product.updated_at,
    )


@router.put(
    "/{product_id}",
    response_model=ProductResponse,
    summary="Update a product",
)
async def update_product(product_id: UUID, request: UpdateProductRequest):
    """Update an existing product's attributes (partial update)."""
    service = _get_service()
    product = await service.update_product(
        product_id=product_id,
        name=request.name,
        description=request.description,
        price=request.price,
        category=request.category,
    )
    if not product:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Product {product_id} not found",
        )
    return ProductResponse(
        id=product.id,
        name=product.name,
        description=product.description,
        price=float(product.price),
        category=product.category,
        sku=product.sku,
        active=product.active,
        created_at=product.created_at,
        updated_at=product.updated_at,
    )


@router.delete(
    "/{product_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete a product",
)
async def delete_product(product_id: UUID):
    """Soft-delete a product (marks as inactive)."""
    service = _get_service()
    deleted = await service.delete_product(product_id)
    if not deleted:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Product {product_id} not found",
        )
