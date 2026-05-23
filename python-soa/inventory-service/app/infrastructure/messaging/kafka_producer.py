"""
Kafka Event Publisher - Inventory Service
===========================================
Publishes inventory response events to Kafka topics.
"""

import json
import logging

from aiokafka import AIOKafkaProducer

logger = logging.getLogger(__name__)


class KafkaEventPublisher:
    """Async Kafka event publisher for inventory events."""

    def __init__(self, brokers: str):
        self._producer = AIOKafkaProducer(
            bootstrap_servers=brokers,
            value_serializer=lambda v: json.dumps(v).encode("utf-8"),
            key_serializer=lambda k: k.encode("utf-8") if k else None,
        )

    async def start(self) -> None:
        """Start the Kafka producer connection."""
        await self._producer.start()
        logger.info("Kafka producer started")

    async def stop(self) -> None:
        """Stop the Kafka producer."""
        await self._producer.stop()
        logger.info("Kafka producer stopped")

    async def publish(self, topic: str, key: str, event: dict) -> None:
        """Publish an event to a Kafka topic."""
        await self._producer.send(topic, key=key, value=event)
        logger.debug("Published event to topic '%s' with key '%s'", topic, key)
