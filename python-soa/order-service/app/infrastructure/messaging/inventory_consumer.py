"""
Inventory Event Consumer
==========================
Listens for inventory response events as part of the Order Saga.

Design Pattern: Saga Participant (Consumer Side)
- Listens for 'stock.reserved' and 'stock.insufficient' events
- Invokes saga callbacks on the Order Service
- Runs as a background task during the application lifecycle

Event Flow:
    Inventory Service → Kafka → This Consumer → Order Service
    
    stock.reserved     → confirm_order() → PENDING → CONFIRMED
    stock.insufficient → cancel_order()  → PENDING → CANCELLED

SOA Principle: Asynchronous Communication
- Non-blocking event consumption
- Services don't need to be available simultaneously
- Kafka provides durability and replay capability
"""

import asyncio
import json
import logging
from uuid import UUID

from aiokafka import AIOKafkaConsumer

logger = logging.getLogger(__name__)


class InventoryEventConsumer:
    """
    Kafka consumer for inventory response events.

    Subscribes to:
    - stock.reserved: Stock was successfully reserved for the order
    - stock.insufficient: Not enough stock available

    These events drive the order saga state transitions.
    """

    def __init__(self, brokers: str, group_id: str, service):
        self._consumer = AIOKafkaConsumer(
            "stock.reserved",
            "stock.insufficient",
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
        logger.info("Inventory event consumer started")

    async def stop(self) -> None:
        """Stop the consumer gracefully."""
        if self._task:
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass
        await self._consumer.stop()
        logger.info("Inventory event consumer stopped")

    async def _consume(self) -> None:
        """
        Main consumption loop.

        Continuously reads messages from Kafka and dispatches
        them to the appropriate handler based on the topic.
        """
        try:
            async for msg in self._consumer:
                try:
                    logger.info(
                        "Received event on topic '%s': key=%s",
                        msg.topic,
                        msg.key.decode("utf-8") if msg.key else None,
                    )

                    if msg.topic == "stock.reserved":
                        await self._handle_stock_reserved(msg.value)
                    elif msg.topic == "stock.insufficient":
                        await self._handle_stock_insufficient(msg.value)

                except Exception as e:
                    logger.error("Error processing message: %s", e, exc_info=True)

        except asyncio.CancelledError:
            logger.info("Consumer task cancelled")
            raise

    async def _handle_stock_reserved(self, event: dict) -> None:
        """
        Handle 'stock.reserved' event - happy path of the saga.

        The Inventory Service has successfully reserved stock for all
        items in the order. Transition order to CONFIRMED.
        """
        order_id = UUID(event["data"]["order_id"])
        logger.info("Stock reserved for order %s - confirming order", order_id)
        await self._service.confirm_order(order_id)

    async def _handle_stock_insufficient(self, event: dict) -> None:
        """
        Handle 'stock.insufficient' event - compensation path.

        The Inventory Service could not reserve stock for the order.
        Transition order to CANCELLED (saga compensation).
        """
        order_id = UUID(event["data"]["order_id"])
        reason = event["data"].get("reason", "Insufficient stock")
        logger.info(
            "Stock insufficient for order %s - cancelling (reason: %s)",
            order_id,
            reason,
        )
        await self._service.cancel_order(order_id, reason)
