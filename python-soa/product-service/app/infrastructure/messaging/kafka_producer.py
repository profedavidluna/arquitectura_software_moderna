"""
Kafka Event Publisher
======================
Publishes domain events to Apache Kafka topics.

Design Pattern: Observer/Pub-Sub
- Services publish events without knowing who consumes them
- Enables loose coupling between services
- New consumers can be added without modifying the publisher

SOA Principle: Service Loose Coupling
- The publisher doesn't know about subscribers
- Communication is asynchronous and non-blocking
- Services can evolve independently

Enterprise Service Bus (ESB) Pattern:
- Kafka acts as the ESB in this SOA architecture
- Provides reliable message delivery
- Supports topic-based routing
"""

import json
import logging

from aiokafka import AIOKafkaProducer

logger = logging.getLogger(__name__)


class KafkaEventPublisher:
    """
    Async Kafka event publisher using aiokafka.

    Serializes events as JSON and publishes them to Kafka topics.
    Uses message keys for partition routing (ensures ordering per entity).
    """

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
        """Stop the Kafka producer and flush pending messages."""
        await self._producer.stop()
        logger.info("Kafka producer stopped")

    async def publish(self, topic: str, key: str, event: dict) -> None:
        """
        Publish an event to a Kafka topic.

        Args:
            topic: The Kafka topic name (e.g., 'product.created')
            key: Message key for partition routing (usually entity ID)
            event: The event payload as a dictionary
        """
        await self._producer.send(topic, key=key, value=event)
        logger.debug("Published event to topic '%s' with key '%s'", topic, key)
