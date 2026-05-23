"""
Order Event Consumer
======================
Listens for order events as part of the Order Saga.

Design Pattern: Saga Participant (Consumer Side)
- Listens for 'order.created' and 'order.cancelled' events
- Invokes saga participant methods on the Inventory Service
- Runs as a background task during the application lifecycle

Event Flow:
    Order Service → Kafka → This Consumer → Inventory Service

    order.created   → reserve_stock()  → stock.reserved / stock.insufficient
    order.cancelled → release_stock()  → stock.released

SOA Principle: Service Loose Coupling
- The Inventory Service doesn't know about the Order Service directly
- Communication happens through well-defined events
- Either service can be updated independently
"""

import asyncio
import json
import logging
from uuid import UUID

from aiokafka import AIOKafkaConsumer

logger = logging.getLogger(__name__)


class OrderEventConsumer:
    """
    Kafka consumer for order-related events.

    Subscribes to:
    - order.created: New order needs stock reservation
    - order.cancelled: Confirmed order cancelled, release stock

    Acts as the entry point for saga participation.
    """

    def __init__(self, brokers: str, group_id: str, service):
        self._consumer = AIOKafkaConsumer(
            "order.created",
            "order.cancelled",
            bootstrap_servers=brokers,
            group_id=group_id,
            value_deserializer=lambda v: json.loads(v.decode("utf-8")),
            auto_offset_reset="earliest",
        )
        self._service = service
        self._task: asyncio.Task | None = None

    async def start(self) -> None:
        """Start the consumer and begin processing messages."""
        await self._consumer.start()
        self._task = asyncio.create_task(self._consume())
        logger.info("Order event consumer started")

    async def stop(self) -> None:
        """Stop the consumer gracefully."""
        if self._task:
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass
        await self._consumer.stop()
        logger.info("Order event consumer stopped")

    async def _consume(self) -> None:
        """
        Main consumption loop.

        Processes order events and delegates to the inventory service
        for saga participation.
        """
        try:
            async for msg in self._consumer:
                try:
                    logger.info(
                        "Received event on topic '%s': key=%s",
                        msg.topic,
                        msg.key.decode("utf-8") if msg.key else None,
                    )

                    if msg.topic == "order.created":
                        await self._handle_order_created(msg.value)
                    elif msg.topic == "order.cancelled":
                        await self._handle_order_cancelled(msg.value)

                except Exception as e:
                    logger.error("Error processing message: %s", e, exc_info=True)

        except asyncio.CancelledError:
            logger.info("Consumer task cancelled")
            raise

    async def _handle_order_created(self, event: dict) -> None:
        """
        Handle 'order.created' event - saga command.

        Attempts to reserve stock for all items in the order.
        The inventory service will publish the appropriate response event.
        """
        order_id = UUID(event["data"]["order_id"])
        items = event["data"]["items"]

        logger.info(
            "Processing order.created for order %s (%d items)",
            order_id, len(items),
        )

        await self._service.reserve_stock(order_id, items)

    async def _handle_order_cancelled(self, event: dict) -> None:
        """
        Handle 'order.cancelled' event - compensation command.

        Releases previously reserved stock for the cancelled order.
        """
        order_id = UUID(event["data"]["order_id"])
        items = event["data"]["items"]

        logger.info(
            "Processing order.cancelled for order %s - releasing stock",
            order_id,
        )

        await self._service.release_stock(order_id, items)
