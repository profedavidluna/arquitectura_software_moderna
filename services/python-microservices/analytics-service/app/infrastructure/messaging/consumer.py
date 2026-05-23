import json
import logging
from typing import Callable, Awaitable
from aiokafka import AIOKafkaConsumer

logger = logging.getLogger(__name__)


class KafkaConsumer:
    def __init__(self, bootstrap_servers: str, topics: list[str],
                 group_id: str, handler: Callable[[dict], Awaitable[None]]):
        self.bootstrap_servers = bootstrap_servers
        self.topics = topics
        self.group_id = group_id
        self.handler = handler
        self.consumer = None
        self._running = False

    async def start(self):
        try:
            self.consumer = AIOKafkaConsumer(
                *self.topics,
                bootstrap_servers=self.bootstrap_servers,
                group_id=self.group_id,
                value_deserializer=lambda v: json.loads(v.decode("utf-8")),
                auto_offset_reset="earliest",
            )
            await self.consumer.start()
            self._running = True
            logger.info(f"Kafka consumer started for topics: {self.topics}")

            async for message in self.consumer:
                if not self._running:
                    break
                try:
                    await self.handler(message.value)
                except Exception as e:
                    logger.error(f"Error handling message: {e}")

        except Exception as e:
            logger.warning(f"Kafka consumer failed: {e}")

    async def stop(self):
        self._running = False
        if self.consumer:
            await self.consumer.stop()
            logger.info("Kafka consumer stopped")
