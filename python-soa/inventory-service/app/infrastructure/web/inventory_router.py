"""
Inventory Router (REST API)
==============================
FastAPI router for Inventory Service endpoints.

Endpoints:
- POST /api/inventory           → Create inventory entry
- GET  /api/inventory           → List all inventory
- GET  /api/inventory/{product_id} → Get inventory by product
- PUT  /api/inventory/{product_id}/stock → Add stock
"""

import logging
from uuid import UUID

from fastapi import APIRouter, HTTPException, status

from app.application.inventory_service import InventoryServiceImpl
from app.infrastructure.web.dto import (
    CreateInventoryRequest,
    AddStockRequest,
    InventoryResponse,
)

logger = logging.getLogger(__name__)

router = APIRouter()

_service: InventoryServiceImpl | None = None


def set_service(service: InventoryServiceImpl) -> None:
    """Inject the service instance (called during app startup)."""
    global _service
    _service = service


def _get_service() -> InventoryServiceImpl:
    """Get the service instance."""
    if _service is None:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Service not initialized",
        )
    return _service


def _to_response(item) -> InventoryResponse:
    """Convert InventoryItem domain object to response DTO."""
    return InventoryResponse(
        id=item.id,
        product_id=item.product_id,
        product_name=item.product_name,
        quantity_available=item.quantity_available,
        quantity_reserved=item.quantity_reserved,
        total_stock=item.total_stock,
        updated_at=item.updated_at,
    )


@router.post(
    "",
    response_model=InventoryResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create inventory entry",
)
async def create_inventory(request: CreateInventoryRequest):
    """
    Create a new inventory entry for a product.

    This is typically called when a new product is added to the catalog,
    or can be triggered automatically by consuming 'product.created' events.
    """
    service = _get_service()
    item = await service.create_inventory(
        product_id=UUID(request.product_id),
        product_name=request.product_name,
        initial_quantity=request.initial_quantity,
    )
    return _to_response(item)


@router.get(
    "",
    response_model=list[InventoryResponse],
    summary="List all inventory",
)
async def list_inventory():
    """Retrieve all inventory items with current stock levels."""
    service = _get_service()
    items = await service.get_all()
    return [_to_response(item) for item in items]


@router.get(
    "/{product_id}",
    response_model=InventoryResponse,
    summary="Get inventory by product",
)
async def get_inventory(product_id: UUID):
    """Retrieve inventory for a specific product."""
    service = _get_service()
    item = await service.get_by_product_id(product_id)
    if not item:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Inventory for product {product_id} not found",
        )
    return _to_response(item)


@router.put(
    "/{product_id}/stock",
    response_model=InventoryResponse,
    summary="Add stock",
)
async def add_stock(product_id: UUID, request: AddStockRequest):
    """
    Add stock to an existing inventory item.

    Increases the quantity_available by the specified amount.
    """
    service = _get_service()
    item = await service.add_stock(product_id, request.quantity)
    if not item:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Inventory for product {product_id} not found",
        )
    return _to_response(item)
