from fastapi import APIRouter, HTTPException, Request

from app.infrastructure.web.dto import (
    CreatePaymentRequest, RefundRequest, PaymentResponse, RefundResponse,
)


def create_router() -> APIRouter:
    router = APIRouter()

    @router.post("", response_model=PaymentResponse, status_code=201)
    async def process_payment(request: Request, body: CreatePaymentRequest):
        service = request.app.state.service
        payment = await service.process_payment(
            order_id=body.orderId, user_id=body.userId,
            amount=body.amount, method=body.method, currency=body.currency,
        )
        return PaymentResponse(
            id=payment.id, order_id=payment.order_id, user_id=payment.user_id,
            amount=payment.amount, currency=payment.currency, method=payment.method,
            status=payment.status, transaction_id=payment.transaction_id,
            failure_reason=payment.failure_reason, retry_count=payment.retry_count,
            created_at=payment.created_at, updated_at=payment.updated_at,
        )

    @router.get("/{payment_id}", response_model=PaymentResponse)
    async def get_payment(request: Request, payment_id: str):
        service = request.app.state.service
        payment = await service.get_payment(payment_id)
        if not payment:
            raise HTTPException(status_code=404, detail="Payment not found")
        return PaymentResponse(
            id=payment.id, order_id=payment.order_id, user_id=payment.user_id,
            amount=payment.amount, currency=payment.currency, method=payment.method,
            status=payment.status, transaction_id=payment.transaction_id,
            failure_reason=payment.failure_reason, retry_count=payment.retry_count,
            created_at=payment.created_at, updated_at=payment.updated_at,
        )

    @router.get("/order/{order_id}", response_model=PaymentResponse)
    async def get_payment_by_order(request: Request, order_id: str):
        service = request.app.state.service
        payment = await service.get_payment_by_order(order_id)
        if not payment:
            raise HTTPException(status_code=404, detail="Payment not found")
        return PaymentResponse(
            id=payment.id, order_id=payment.order_id, user_id=payment.user_id,
            amount=payment.amount, currency=payment.currency, method=payment.method,
            status=payment.status, transaction_id=payment.transaction_id,
            failure_reason=payment.failure_reason, retry_count=payment.retry_count,
            created_at=payment.created_at, updated_at=payment.updated_at,
        )

    @router.post("/{payment_id}/refund", response_model=RefundResponse, status_code=201)
    async def refund_payment(request: Request, payment_id: str, body: RefundRequest):
        service = request.app.state.service
        try:
            refund = await service.refund_payment(
                payment_id=payment_id, amount=body.amount, reason=body.reason,
            )
            return RefundResponse(
                id=refund.id, payment_id=refund.payment_id,
                amount=refund.amount, reason=refund.reason,
                status=refund.status, created_at=refund.created_at,
            )
        except ValueError as e:
            raise HTTPException(status_code=400, detail=str(e))

    return router
