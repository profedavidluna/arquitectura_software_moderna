import logging
from typing import Optional
from dataclasses import dataclass

import httpx

logger = logging.getLogger(__name__)


@dataclass
class SagaResult:
    success: bool
    step: str
    data: Optional[dict] = None
    error: Optional[str] = None


class SagaOrchestrator:
    """
    Orchestrates the order creation saga:
    1. Validate cart → 2. Reserve inventory → 3. Process payment → 4. Confirm order

    Compensation (rollback) on failure:
    - Payment failed → Release inventory
    - Inventory failed → Restore cart
    """

    def __init__(self, http_client: httpx.AsyncClient,
                 cart_service_url: str, inventory_service_url: str,
                 payment_service_url: str):
        self.http_client = http_client
        self.cart_service_url = cart_service_url
        self.inventory_service_url = inventory_service_url
        self.payment_service_url = payment_service_url

    async def execute(self, order_id: str, cart_id: str, user_id: str,
                      items: list[dict], total_amount: float) -> list[SagaResult]:
        results: list[SagaResult] = []

        # Step 1: Validate and checkout cart
        cart_result = await self._checkout_cart(cart_id)
        results.append(cart_result)
        if not cart_result.success:
            return results

        # Step 2: Reserve inventory
        inventory_result = await self._reserve_inventory(order_id, items)
        results.append(inventory_result)
        if not inventory_result.success:
            # Compensate: restore cart
            await self._compensate_cart(cart_id)
            return results

        # Step 3: Process payment
        payment_result = await self._process_payment(order_id, user_id, total_amount)
        results.append(payment_result)
        if not payment_result.success:
            # Compensate: release inventory, restore cart
            await self._compensate_inventory(order_id, items)
            await self._compensate_cart(cart_id)
            return results

        # Step 4: Confirm (all steps succeeded)
        results.append(SagaResult(success=True, step="CONFIRM", data={"orderId": order_id}))
        return results

    async def _checkout_cart(self, cart_id: str) -> SagaResult:
        try:
            response = await self.http_client.post(
                f"{self.cart_service_url}/api/carts/{cart_id}/checkout"
            )
            response.raise_for_status()
            return SagaResult(success=True, step="CART_CHECKOUT", data=response.json())
        except Exception as e:
            logger.error(f"Saga: Cart checkout failed: {e}")
            return SagaResult(success=False, step="CART_CHECKOUT", error=str(e))

    async def _reserve_inventory(self, order_id: str, items: list[dict]) -> SagaResult:
        try:
            response = await self.http_client.post(
                f"{self.inventory_service_url}/api/inventory/reserve",
                json={"orderId": order_id, "items": items},
            )
            response.raise_for_status()
            return SagaResult(success=True, step="INVENTORY_RESERVE", data=response.json())
        except Exception as e:
            logger.error(f"Saga: Inventory reservation failed: {e}")
            return SagaResult(success=False, step="INVENTORY_RESERVE", error=str(e))

    async def _process_payment(self, order_id: str, user_id: str, amount: float) -> SagaResult:
        try:
            response = await self.http_client.post(
                f"{self.payment_service_url}/api/payments",
                json={
                    "orderId": order_id, "userId": user_id,
                    "amount": amount, "method": "CREDIT_CARD",
                },
            )
            response.raise_for_status()
            return SagaResult(success=True, step="PAYMENT_PROCESS", data=response.json())
        except Exception as e:
            logger.error(f"Saga: Payment processing failed: {e}")
            return SagaResult(success=False, step="PAYMENT_PROCESS", error=str(e))

    async def _compensate_cart(self, cart_id: str) -> None:
        try:
            logger.info(f"Saga compensation: Restoring cart {cart_id}")
            # In a real system, this would revert the cart status
        except Exception as e:
            logger.error(f"Saga compensation failed for cart: {e}")

    async def _compensate_inventory(self, order_id: str, items: list[dict]) -> None:
        try:
            logger.info(f"Saga compensation: Releasing inventory for order {order_id}")
            await self.http_client.post(
                f"{self.inventory_service_url}/api/inventory/release",
                json={"orderId": order_id, "items": items},
            )
        except Exception as e:
            logger.error(f"Saga compensation failed for inventory: {e}")
