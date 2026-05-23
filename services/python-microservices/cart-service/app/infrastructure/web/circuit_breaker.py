import time
import logging
from typing import Callable, Any

logger = logging.getLogger(__name__)


class CircuitBreakerOpenError(Exception):
    def __init__(self, name: str):
        self.name = name
        super().__init__(f"Circuit breaker '{name}' is OPEN")


class CircuitBreaker:
    """
    Circuit Breaker pattern implementation.
    States: CLOSED -> OPEN -> HALF_OPEN -> CLOSED
    """

    def __init__(self, name: str, threshold: int = 5, timeout: float = 30.0):
        self.name = name
        self.state = "CLOSED"
        self.failure_count = 0
        self.threshold = threshold
        self.timeout = timeout
        self.last_failure_time = 0.0
        self.success_count = 0

    async def execute(self, fn: Callable[[], Any]):
        if self.state == "OPEN":
            if time.time() - self.last_failure_time > self.timeout:
                logger.info(f"Circuit breaker '{self.name}' transitioning to HALF_OPEN")
                self.state = "HALF_OPEN"
            else:
                raise CircuitBreakerOpenError(self.name)

        try:
            result = await fn()
            self._on_success()
            return result
        except Exception as e:
            self._on_failure()
            raise

    def _on_success(self):
        if self.state == "HALF_OPEN":
            self.success_count += 1
            if self.success_count >= 3:
                logger.info(f"Circuit breaker '{self.name}' transitioning to CLOSED")
                self.state = "CLOSED"
                self.failure_count = 0
                self.success_count = 0
        else:
            self.failure_count = 0

    def _on_failure(self):
        self.failure_count += 1
        self.last_failure_time = time.time()
        self.success_count = 0

        if self.failure_count >= self.threshold:
            logger.warning(f"Circuit breaker '{self.name}' transitioning to OPEN")
            self.state = "OPEN"

    @property
    def is_open(self) -> bool:
        return self.state == "OPEN"
