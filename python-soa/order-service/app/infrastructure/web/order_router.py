"""
Order Router (REST API)
=========================
FastAPI router for Order Service endpoints.

Endpoints:
- POST /api/orders          → Create order (initiates saga)
- GET  /api/orders/{id}     → Get order by ID
- GET  /api/orders/user/{id} → Get orders by user
- POST /api/orders/{id}/cancel → Cancel order (triggers compensation)
"""

import logging
from uuid import UUID

from fastapi import APIRouter, HTTPException, status

from app.application.order_service import OrderServiceImpl
from app.infrastructure.web.dto import (
    CreateOrderRequest,
    CancelOrderRequest,
    OrderResponse,
    OrderItemResponse,
)

logger = logging.getLogger(__name__)

router = APIRouter()

_service: OrderServiceImpl | None = None


def set_service(service: OrderServiceImpl) -> None:
    """Inject the service instance (called during app startup)."""
    global _service
    _service = service


def _get_service() -> OrderServiceImpl:
    """Get the service instance."""
    if _service is None:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Service not initialized",
        )
    return _service


def _to_response(order) -> OrderResponse:
    """Convert Order domain object to response DTO."""
    return OrderResponse(
        id=order.id,
        user_id=order.user_id,
        status=order.status.value,
        total_amount=float(order.total_amount),
        items=[
            OrderItemResponse(
                id=item.id,
                product_id=item.product_id,
                product_name=item.product_name,
                quantity=item.quantity,
                unit_price=float(item.unit_price),
                subtotal=float(item.subtotal),
            )
            for item in order.items
        ],
        created_at=order.created_at,
        updated_at=order.updated_at,
    )


@router.post(
    "",
    response_model=OrderResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new order",
)
async def create_order(request: CreateOrderRequest):
    """
    Create a new order and initiate the order creation saga.

    The order starts in PENDING status. The saga will:
    1. Publish 'order.created' event
    2. Inventory Service reserves stock
    3. Order transitions to CONFIRMED or CANCELLED asynchronously
    """
    service = _get_service()

    items = [
        {
            "product_id": item.product_id,
            "product_name": item.product_name,
            "quantity": item.quantity,
            "unit_price": item.unit_price,
        }
        for item in request.items
    ]

    order = await service.create_order(
        user_id=UUID(request.user_id),
        items=items,
    )

    return _to_response(order)


@router.get(
    "/{order_id}",
    response_model=OrderResponse,
    summary="Get order by ID",
)
async def get_order(order_id: UUID):
    """Retrieve a specific order with its items and current saga status."""
    service = _get_service()
    order = await service.get_by_id(order_id)
    if not order:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Order {order_id} not found",
        )
    return _to_response(order)


@router.get(
    "/user/{user_id}",
    response_model=list[OrderResponse],
    summary="Get orders by user",
)
async def get_user_orders(user_id: UUID):
    """Retrieve all orders for a specific user."""
    service = _get_service()
    orders = await service.get_by_user(user_id)
    return [_to_response(order) for order in orders]


@router.post(
    "/{order_id}/cancel",
    response_model=OrderResponse,
    summary="Cancel an order",
)
async def cancel_order(order_id: UUID, request: CancelOrderRequest = CancelOrderRequest()):
    """
    Cancel an order manually.

    If the order was CONFIRMED, this triggers the compensation flow:
    - Publishes 'order.cancelled' event
    - Inventory Service releases reserved stock
    """
    service = _get_service()

    order = await service.get_by_id(order_id)
    if not order:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Order {order_id} not found",
        )

    await service.cancel_order(order_id, request.reason)

    # Reload to get updated status
    updated_order = await service.get_by_id(order_id)
    return _to_response(updated_order)
