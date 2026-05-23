import asyncio
import logging
import uuid
from typing import Optional
from decimal import Decimal

from app.domain.models import Payment, Refund
from app.domain.interfaces import PaymentRepositoryProtocol, EventProducerProtocol

logger = logging.getLogger(__name__)


class PaymentProcessingError(Exception):
    pass


class PaymentService:
    def __init__(self, repository: PaymentRepositoryProtocol,
                 producer: EventProducerProtocol,
                 max_retries: int = 3, retry_base_delay: float = 1.0):
        self.repository = repository
        self.producer = producer
        self.max_retries = max_retries
        self.retry_base_delay = retry_base_delay

    async def process_payment(self, order_id: str, user_id: str,
                              amount: Decimal, method: str = "CREDIT_CARD",
                              currency: str = "USD") -> Payment:
        payment = Payment(
            order_id=order_id, user_id=user_id,
            amount=amount, method=method, currency=currency,
        )
        created = await self.repository.create(payment)

        # Process with retry and exponential backoff
        success = await self._process_with_retry(created)

        if success:
            transaction_id = str(uuid.uuid4())
            await self.repository.update_status(created.id, "COMPLETED", transaction_id)
            await self.producer.publish(
                topic="payment-events",
                key=created.id,
                value={
                    "event": "PAYMENT_COMPLETED",
                    "paymentId": created.id,
                    "orderId": order_id,
                    "amount": str(amount),
                    "transactionId": transaction_id,
                },
            )
            created.status = "COMPLETED"
            created.transaction_id = transaction_id
        else:
            await self.repository.update_status(created.id, "FAILED")
            await self.producer.publish(
                topic="payment-events",
                key=created.id,
                value={
                    "event": "PAYMENT_FAILED",
                    "paymentId": created.id,
                    "orderId": order_id,
                },
            )
            created.status = "FAILED"

        return created

    async def _process_with_retry(self, payment: Payment) -> bool:
        """Process payment with exponential backoff retry."""
        for attempt in range(self.max_retries):
            try:
                # Simulate payment gateway call
                await self._call_payment_gateway(payment)
                return True
            except PaymentProcessingError as e:
                delay = self.retry_base_delay * (2 ** attempt)
                logger.warning(
                    f"Payment attempt {attempt + 1}/{self.max_retries} failed: {e}. "
                    f"Retrying in {delay}s..."
                )
                await self.repository.update_retry(
                    payment.id, attempt + 1, str(e)
                )
                if attempt < self.max_retries - 1:
                    await asyncio.sleep(delay)

        return False

    async def _call_payment_gateway(self, payment: Payment) -> None:
        """
        Simulates calling an external payment gateway.
        In production, this would integrate with Stripe, PayPal, etc.
        """
        # Simulate: payments under $1 always fail (for testing)
        if payment.amount < Decimal("1.00"):
            raise PaymentProcessingError("Amount too low for processing")

        # Simulate successful processing
        logger.info(f"Payment {payment.id} processed successfully: ${payment.amount}")

    async def get_payment(self, payment_id: str) -> Optional[Payment]:
        return await self.repository.find_by_id(payment_id)

    async def get_payment_by_order(self, order_id: str) -> Optional[Payment]:
        return await self.repository.find_by_order(order_id)

    async def refund_payment(self, payment_id: str, amount: Optional[Decimal] = None,
                             reason: Optional[str] = None) -> Refund:
        payment = await self.repository.find_by_id(payment_id)
        if not payment:
            raise ValueError(f"Payment {payment_id} not found")
        if payment.status != "COMPLETED":
            raise ValueError(f"Cannot refund payment in status {payment.status}")

        refund_amount = amount or payment.amount
        if refund_amount > payment.amount:
            raise ValueError("Refund amount exceeds payment amount")

        refund = Refund(
            payment_id=payment_id,
            amount=refund_amount,
            reason=reason,
            status="COMPLETED",
        )
        created = await self.repository.create_refund(refund)

        await self.repository.update_status(payment_id, "REFUNDED")

        await self.producer.publish(
            topic="payment-events",
            key=payment_id,
            value={
                "event": "PAYMENT_REFUNDED",
                "paymentId": payment_id,
                "refundId": created.id,
                "amount": str(refund_amount),
            },
        )
        return created
