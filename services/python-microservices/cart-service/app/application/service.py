import logging
from typing import Optional
from decimal import Decimal

import httpx

from app.domain.models import Cart, CartItem
from app.domain.interfaces import CartRepositoryProtocol, EventProducerProtocol, CacheProtocol
from app.infrastructure.web.circuit_breaker import CircuitBreaker

logger = logging.getLogger(__name__)

VALID_COUPONS = {
    "SAVE10": Decimal("10"),
    "SAVE20": Decimal("20"),
    "WELCOME": Decimal("15"),
}


class CartService:
    def __init__(self, repository: CartRepositoryProtocol, producer: EventProducerProtocol,
                 cache: CacheProtocol, http_client: httpx.AsyncClient,
                 circuit_breaker: CircuitBreaker, product_service_url: str):
        self.repository = repository
        self.producer = producer
        self.cache = cache
        self.http_client = http_client
        self.circuit_breaker = circuit_breaker
        self.product_service_url = product_service_url

    async def get_or_create_cart(self, user_id: str) -> Cart:
        cart = await self.repository.find_by_user(user_id)
        if not cart:
            cart = Cart(user_id=user_id)
            cart = await self.repository.create(cart)
        return cart

    async def get_cart(self, cart_id: str) -> Optional[Cart]:
        return await self.repository.find_by_id(cart_id)

    async def add_item(self, cart_id: str, product_id: str, quantity: int = 1) -> Cart:
        cart = await self.repository.find_by_id(cart_id)
        if not cart:
            raise ValueError(f"Cart {cart_id} not found")

        # Fetch product info via circuit breaker
        product_info = await self._fetch_product(product_id)
        if not product_info:
            raise ValueError(f"Product {product_id} not available")

        # Check if item already in cart
        existing_item = next((i for i in cart.items if i.product_id == product_id), None)
        if existing_item:
            new_qty = existing_item.quantity + quantity
            await self.repository.update_item_quantity(existing_item.id, new_qty)
        else:
            item = CartItem(
                cart_id=cart_id,
                product_id=product_id,
                product_name=product_info["name"],
                price=Decimal(str(product_info["price"])),
                quantity=quantity,
            )
            await self.repository.add_item(item)

        updated_cart = await self.repository.find_by_id(cart_id)

        await self.producer.publish(
            topic="cart-events",
            key=cart_id,
            value={"event": "ITEM_ADDED", "cartId": cart_id, "productId": product_id},
        )
        return updated_cart

    async def remove_item(self, cart_id: str, item_id: str) -> Cart:
        await self.repository.remove_item(item_id)
        cart = await self.repository.find_by_id(cart_id)

        await self.producer.publish(
            topic="cart-events",
            key=cart_id,
            value={"event": "ITEM_REMOVED", "cartId": cart_id, "itemId": item_id},
        )
        return cart

    async def apply_coupon(self, cart_id: str, coupon_code: str) -> Cart:
        discount = VALID_COUPONS.get(coupon_code.upper())
        if not discount:
            raise ValueError(f"Invalid coupon code: {coupon_code}")

        await self.repository.apply_coupon(cart_id, coupon_code.upper(), float(discount))
        return await self.repository.find_by_id(cart_id)

    async def clear_cart(self, cart_id: str) -> None:
        await self.repository.clear_cart(cart_id)

    async def checkout(self, cart_id: str) -> Cart:
        cart = await self.repository.find_by_id(cart_id)
        if not cart or not cart.items:
            raise ValueError("Cart is empty or not found")

        await self.repository.update_status(cart_id, "CHECKED_OUT")
        cart.status = "CHECKED_OUT"

        await self.producer.publish(
            topic="cart-events",
            key=cart_id,
            value={
                "event": "CART_CHECKED_OUT",
                "cartId": cart_id,
                "userId": cart.user_id,
                "total": str(cart.total),
            },
        )
        return cart

    async def _fetch_product(self, product_id: str) -> Optional[dict]:
        async def call():
            response = await self.http_client.get(
                f"{self.product_service_url}/api/products/{product_id}"
            )
            response.raise_for_status()
            return response.json()

        try:
            return await self.circuit_breaker.execute(call)
        except Exception as e:
            logger.error(f"Failed to fetch product {product_id}: {e}")
            return None
