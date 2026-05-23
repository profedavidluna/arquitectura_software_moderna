from fastapi import APIRouter, HTTPException, Request

from app.infrastructure.web.dto import (
    CreateOrderRequest, OrderResponse, OrderItemResponse,
)


def _order_to_response(order) -> OrderResponse:
    return OrderResponse(
        id=order.id, user_id=order.user_id, status=order.status,
        total_amount=order.total_amount, shipping_address=order.shipping_address,
        saga_status=order.saga_status,
        items=[
            OrderItemResponse(
                id=item.id, product_id=item.product_id,
                product_name=item.product_name, price=item.price,
                quantity=item.quantity,
            )
            for item in order.items
        ],
        created_at=order.created_at, updated_at=order.updated_at,
    )


def create_router() -> APIRouter:
    router = APIRouter()

    @router.post("", response_model=OrderResponse, status_code=201)
    async def create_order(request: Request, body: CreateOrderRequest):
        service = request.app.state.service
        items = [item.model_dump() for item in body.items]
        order = await service.create_order(
            user_id=body.user_id, cart_id=body.cart_id,
            items=items, total_amount=body.total_amount,
            shipping_address=body.shipping_address,
        )
        return _order_to_response(order)

    @router.get("/{order_id}", response_model=OrderResponse)
    async def get_order(request: Request, order_id: str):
        service = request.app.state.service
        order = await service.get_order(order_id)
        if not order:
            raise HTTPException(status_code=404, detail="Order not found")
        return _order_to_response(order)

    @router.get("/user/{user_id}", response_model=list[OrderResponse])
    async def get_user_orders(request: Request, user_id: str):
        service = request.app.state.service
        orders = await service.get_user_orders(user_id)
        return [_order_to_response(o) for o in orders]

    @router.post("/{order_id}/cancel", response_model=OrderResponse)
    async def cancel_order(request: Request, order_id: str):
        service = request.app.state.service
        try:
            order = await service.cancel_order(order_id)
            if not order:
                raise HTTPException(status_code=404, detail="Order not found")
            return _order_to_response(order)
        except ValueError as e:
            raise HTTPException(status_code=400, detail=str(e))

    return router
