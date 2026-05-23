import logging
from typing import Optional
from decimal import Decimal
from datetime import datetime

from app.domain.models import Order, OrderItem, SagaStep
from app.domain.interfaces import OrderRepositoryProtocol, EventProducerProtocol
from app.application.saga import SagaOrchestrator

logger = logging.getLogger(__name__)


class OrderService:
    def __init__(self, repository: OrderRepositoryProtocol,
                 producer: EventProducerProtocol, saga: SagaOrchestrator):
        self.repository = repository
        self.producer = producer
        self.saga = saga

    async def create_order(self, user_id: str, cart_id: str, items: list[dict],
                           total_amount: Decimal, shipping_address: Optional[dict] = None) -> Order:
        # Create order in PENDING state
        order = Order(
            user_id=user_id,
            total_amount=total_amount,
            shipping_address=shipping_address,
            items=[
                OrderItem(
                    product_id=item["productId"],
                    product_name=item["productName"],
                    price=Decimal(str(item["price"])),
                    quantity=item["quantity"],
                )
                for item in items
            ],
        )
        created = await self.repository.create(order)

        await self.producer.publish(
            topic="order-events",
            key=created.id,
            value={"event": "ORDER_CREATED", "orderId": created.id, "userId": user_id},
        )

        # Execute saga
        saga_items = [
            {"productId": item["productId"], "quantity": item["quantity"]}
            for item in items
        ]
        saga_results = await self.saga.execute(
            order_id=created.id, cart_id=cart_id, user_id=user_id,
            items=saga_items, total_amount=float(total_amount),
        )

        # Record saga steps
        all_success = True
        for result in saga_results:
            step = SagaStep(
                order_id=created.id,
                step_name=result.step,
                status="COMPLETED" if result.success else "FAILED",
                request_payload=result.data,
                executed_at=datetime.now(),
            )
            await self.repository.save_saga_step(step)
            if not result.success:
                all_success = False

        # Update order status based on saga outcome
        if all_success:
            await self.repository.update_status(created.id, "CONFIRMED")
            await self.repository.update_saga_status(created.id, "COMPLETED")
            await self.producer.publish(
                topic="order-events",
                key=created.id,
                value={"event": "ORDER_CONFIRMED", "orderId": created.id},
            )
        else:
            await self.repository.update_status(created.id, "FAILED")
            await self.repository.update_saga_status(created.id, "COMPENSATED")
            await self.producer.publish(
                topic="order-events",
                key=created.id,
                value={"event": "ORDER_FAILED", "orderId": created.id},
            )

        return await self.repository.find_by_id(created.id)

    async def get_order(self, order_id: str) -> Optional[Order]:
        return await self.repository.find_by_id(order_id)

    async def get_user_orders(self, user_id: str) -> list[Order]:
        return await self.repository.find_by_user(user_id)

    async def cancel_order(self, order_id: str) -> Optional[Order]:
        order = await self.repository.find_by_id(order_id)
        if not order:
            return None
        if order.status not in ("PENDING", "CONFIRMED"):
            raise ValueError(f"Cannot cancel order in status {order.status}")

        await self.repository.update_status(order_id, "CANCELLED")

        await self.producer.publish(
            topic="order-events",
            key=order_id,
            value={"event": "ORDER_CANCELLED", "orderId": order_id},
        )
        return await self.repository.find_by_id(order_id)
