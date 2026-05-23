"""
Order Service Implementation
===============================
Implements the order creation saga orchestration.

Design Pattern: Saga (Orchestration)
======================================
The Saga pattern manages distributed transactions across services
without using traditional 2-phase commit (which doesn't scale in SOA).

Saga Flow:
1. Client creates order → status = PENDING
2. Order Service publishes 'order.created' event
3. Inventory Service attempts to reserve stock
4. If stock available → publishes 'stock.reserved'
   - Order Service confirms order → status = CONFIRMED
5. If stock insufficient → publishes 'stock.insufficient'
   - Order Service cancels order → status = CANCELLED (compensation)

Compensation:
- If a confirmed order is cancelled, 'order.cancelled' is published
- Inventory Service releases the reserved stock

This ensures eventual consistency across the distributed system.
"""

import logging
from decimal import Decimal
from uuid import UUID, uuid4

from app.domain.model import Order, OrderItem, OrderStatus
from app.infrastructure.persistence.order_repository import OrderRepository
from app.infrastructure.messaging.kafka_producer import KafkaEventPublisher
from app.infrastructure.messaging.events import OrderEvents

logger = logging.getLogger(__name__)


class OrderServiceImpl:
    """
    Order Service implementation with Saga orchestration.

    Acts as the saga orchestrator, coordinating the distributed
    transaction between Order and Inventory services.
    """

    def __init__(self, repository: OrderRepository, publisher: KafkaEventPublisher):
        self._repository = repository
        self._publisher = publisher

    async def create_order(
        self,
        user_id: UUID,
        items: list[dict],
    ) -> Order:
        """
        Create a new order and initiate the order creation saga.

        Steps:
        1. Create Order aggregate with items
        2. Persist to database with PENDING status
        3. Publish 'order.created' event to trigger inventory reservation

        The saga continues asynchronously when the Inventory Service
        responds with either 'stock.reserved' or 'stock.insufficient'.
        """
        order_id = uuid4()

        # Build order items
        order_items = []
        for item_data in items:
            unit_price = Decimal(str(item_data["unit_price"]))
            quantity = item_data["quantity"]
            subtotal = unit_price * quantity

            order_item = OrderItem(
                id=uuid4(),
                order_id=order_id,
                product_id=UUID(item_data["product_id"]),
                product_name=item_data.get("product_name", ""),
                quantity=quantity,
                unit_price=unit_price,
                subtotal=subtotal,
            )
            order_items.append(order_item)

        # Create order aggregate
        order = Order(
            id=order_id,
            user_id=user_id,
            status=OrderStatus.PENDING,
            items=order_items,
        )
        order.calculate_total()

        # Persist order
        await self._repository.save(order)
        logger.info(
            "Order created: %s (user: %s, total: %s, status: PENDING)",
            order.id, order.user_id, order.total_amount,
        )

        # Publish event to initiate saga (Inventory Service will consume this)
        event = OrderEvents.order_created(order)
        await self._publisher.publish("order.created", str(order.id), event)
        logger.info("Published order.created event - saga initiated for order %s", order.id)

        return order

    async def get_by_id(self, order_id: UUID) -> Order | None:
        """Retrieve an order with its items."""
        return await self._repository.find_by_id(order_id)

    async def get_by_user(self, user_id: UUID) -> list[Order]:
        """Retrieve all orders for a user."""
        return await self._repository.find_by_user(user_id)

    async def confirm_order(self, order_id: UUID) -> None:
        """
        Saga callback: Confirm order after successful stock reservation.

        Called by the InventoryEventConsumer when 'stock.reserved' is received.
        Transitions order from PENDING → CONFIRMED.
        Publishes 'order.confirmed' event.
        """
        order = await self._repository.find_by_id(order_id)
        if not order:
            logger.error("Cannot confirm order %s: not found", order_id)
            return

        try:
            order.confirm()
            await self._repository.update_status(order_id, order.status)
            logger.info("Order confirmed: %s (saga completed successfully)", order_id)

            # Publish confirmation event
            event = OrderEvents.order_confirmed(order)
            await self._publisher.publish("order.confirmed", str(order_id), event)

        except ValueError as e:
            logger.warning("Cannot confirm order %s: %s", order_id, e)

    async def cancel_order(self, order_id: UUID, reason: str = "") -> None:
        """
        Saga callback/API: Cancel an order.

        Called when:
        - 'stock.insufficient' event received (saga compensation)
        - User manually cancels via API

        If the order was CONFIRMED, publishes 'order.cancelled' to trigger
        stock release in the Inventory Service (compensation).
        """
        order = await self._repository.find_by_id(order_id)
        if not order:
            logger.error("Cannot cancel order %s: not found", order_id)
            return

        was_confirmed = order.status == OrderStatus.CONFIRMED

        try:
            order.cancel()
            await self._repository.update_status(order_id, order.status)
            logger.info("Order cancelled: %s (reason: %s)", order_id, reason)

            # If order was confirmed, we need to release reserved stock
            if was_confirmed:
                event = OrderEvents.order_cancelled(order, reason)
                await self._publisher.publish("order.cancelled", str(order_id), event)
                logger.info(
                    "Published order.cancelled event for stock release (order: %s)",
                    order_id,
                )

        except ValueError as e:
            logger.warning("Cannot cancel order %s: %s", order_id, e)
