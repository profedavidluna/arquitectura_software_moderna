from fastapi import APIRouter, HTTPException, Request

from app.infrastructure.web.dto import (
    AddStockRequest, ReserveRequest, ReleaseRequest,
    InventoryResponse, ReservationResponse,
)
from app.application.service import InsufficientStockError


def create_router() -> APIRouter:
    router = APIRouter()

    @router.post("/stock", response_model=InventoryResponse, status_code=201)
    async def add_stock(request: Request, body: AddStockRequest):
        service = request.app.state.service
        item = await service.add_stock(
            product_id=body.product_id, quantity=body.quantity,
            warehouse_location=body.warehouse_location,
        )
        return InventoryResponse(
            id=item.id, product_id=item.product_id, quantity=item.quantity,
            reserved=item.reserved, available=item.available,
            warehouse_location=item.warehouse_location, updated_at=item.updated_at,
        )

    @router.get("/stock/{product_id}", response_model=InventoryResponse)
    async def get_stock(request: Request, product_id: str):
        service = request.app.state.service
        item = await service.get_stock(product_id)
        if not item:
            raise HTTPException(status_code=404, detail="Inventory not found")
        return InventoryResponse(
            id=item.id, product_id=item.product_id, quantity=item.quantity,
            reserved=item.reserved, available=item.available,
            warehouse_location=item.warehouse_location, updated_at=item.updated_at,
        )

    @router.get("/stock", response_model=list[InventoryResponse])
    async def list_stock(request: Request):
        service = request.app.state.service
        items = await service.get_all_stock()
        return [
            InventoryResponse(
                id=i.id, product_id=i.product_id, quantity=i.quantity,
                reserved=i.reserved, available=i.available,
                warehouse_location=i.warehouse_location, updated_at=i.updated_at,
            )
            for i in items
        ]

    @router.post("/reserve", response_model=list[ReservationResponse], status_code=201)
    async def reserve_stock(request: Request, body: ReserveRequest):
        service = request.app.state.service
        try:
            items = [{"productId": i.productId, "quantity": i.quantity} for i in body.items]
            reservations = await service.reserve_items(body.orderId, items)
            return [
                ReservationResponse(
                    id=r.id, order_id=r.order_id, product_id=r.product_id,
                    quantity=r.quantity, status=r.status, created_at=r.created_at,
                )
                for r in reservations
            ]
        except InsufficientStockError as e:
            raise HTTPException(status_code=409, detail=str(e))

    @router.post("/release", status_code=200)
    async def release_stock(request: Request, body: ReleaseRequest):
        service = request.app.state.service
        items = [{"productId": i.productId, "quantity": i.quantity} for i in body.items]
        await service.release_items(body.orderId, items)
        return {"message": "Stock released successfully"}

    return router
