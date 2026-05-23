from fastapi import APIRouter, HTTPException, Request, Query

from app.infrastructure.web.dto import (
    AddItemRequest, ApplyCouponRequest, CartResponse, CartItemResponse,
)


def _cart_to_response(cart) -> CartResponse:
    return CartResponse(
        id=cart.id, user_id=cart.user_id, status=cart.status,
        coupon_code=cart.coupon_code, discount_percent=cart.discount_percent,
        items=[
            CartItemResponse(
                id=item.id, product_id=item.product_id,
                product_name=item.product_name, price=item.price,
                quantity=item.quantity,
            )
            for item in cart.items
        ],
        subtotal=cart.subtotal, total=cart.total,
        created_at=cart.created_at, updated_at=cart.updated_at,
    )


def create_router() -> APIRouter:
    router = APIRouter()

    @router.post("/user/{user_id}", response_model=CartResponse)
    async def get_or_create_cart(request: Request, user_id: str):
        service = request.app.state.service
        cart = await service.get_or_create_cart(user_id)
        return _cart_to_response(cart)

    @router.get("/{cart_id}", response_model=CartResponse)
    async def get_cart(request: Request, cart_id: str):
        service = request.app.state.service
        cart = await service.get_cart(cart_id)
        if not cart:
            raise HTTPException(status_code=404, detail="Cart not found")
        return _cart_to_response(cart)

    @router.post("/{cart_id}/items", response_model=CartResponse)
    async def add_item(request: Request, cart_id: str, body: AddItemRequest):
        service = request.app.state.service
        try:
            cart = await service.add_item(cart_id, body.product_id, body.quantity)
            return _cart_to_response(cart)
        except ValueError as e:
            raise HTTPException(status_code=400, detail=str(e))

    @router.delete("/{cart_id}/items/{item_id}", response_model=CartResponse)
    async def remove_item(request: Request, cart_id: str, item_id: str):
        service = request.app.state.service
        cart = await service.remove_item(cart_id, item_id)
        return _cart_to_response(cart)

    @router.post("/{cart_id}/coupon", response_model=CartResponse)
    async def apply_coupon(request: Request, cart_id: str, body: ApplyCouponRequest):
        service = request.app.state.service
        try:
            cart = await service.apply_coupon(cart_id, body.coupon_code)
            return _cart_to_response(cart)
        except ValueError as e:
            raise HTTPException(status_code=400, detail=str(e))

    @router.post("/{cart_id}/checkout", response_model=CartResponse)
    async def checkout(request: Request, cart_id: str):
        service = request.app.state.service
        try:
            cart = await service.checkout(cart_id)
            return _cart_to_response(cart)
        except ValueError as e:
            raise HTTPException(status_code=400, detail=str(e))

    @router.delete("/{cart_id}", status_code=204)
    async def clear_cart(request: Request, cart_id: str):
        service = request.app.state.service
        await service.clear_cart(cart_id)

    return router
