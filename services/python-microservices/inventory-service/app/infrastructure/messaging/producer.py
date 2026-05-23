import json
import logging
from aiokafka import AIOKafkaProducer

logger = logging.getLogger(__name__)


class KafkaProducer:
    def __init__(self, bootstrap_servers: str):
        self.bootstrap_servers = bootstrap_servers
        self.producer = None

    async def start(self):
        try:
            self.producer = AIOKafkaProducer(
                bootstrap_servers=self.bootstrap_servers,
                value_serializer=lambda v: json.dumps(v).encode("utf-8"),
                key_serializer=lambda k: k.encode("utf-8") if k else None,
            )
            await self.producer.start()
            logger.info("Kafka producer started")
        except Exception as e:
            logger.warning(f"Failed to connect to Kafka: {e}. Running without messaging.")
            self.producer = None

    async def stop(self):
        if self.producer:
            await self.producer.stop()

    async def publish(self, topic: str, key: str, value: dict) -> None:
        if not self.producer:
            logger.warning(f"Kafka not available. Event lost: {topic}/{key}")
            return
        try:
            await self.producer.send_and_wait(topic, value=value, key=key)
        except Exception as e:
            logger.error(f"Failed to publish event: {e}")
