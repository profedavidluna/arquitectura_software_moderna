from typing import Optional
from decimal import Decimal
from app.domain.models import Cart, CartItem
from app.infrastructure.persistence.database import Database


class CartRepository:
    def __init__(self, db: Database):
        self.db = db

    async def create(self, cart: Cart) -> Cart:
        query = """
            INSERT INTO carts (id, user_id, status, coupon_code, discount_percent)
            VALUES ($1, $2, $3, $4, $5) RETURNING *
        """
        row = await self.db.fetch_one(
            query, cart.id, cart.user_id, cart.status,
            cart.coupon_code, cart.discount_percent
        )
        return Cart(
            id=str(row["id"]), user_id=str(row["user_id"]),
            status=row["status"], coupon_code=row["coupon_code"],
            discount_percent=Decimal(str(row["discount_percent"])),
            created_at=row["created_at"], updated_at=row["updated_at"],
        )

    async def find_by_id(self, cart_id: str) -> Optional[Cart]:
        row = await self.db.fetch_one("SELECT * FROM carts WHERE id = $1", cart_id)
        if not row:
            return None

        items_rows = await self.db.fetch_all(
            "SELECT * FROM cart_items WHERE cart_id = $1", cart_id
        )
        items = [
            CartItem(
                id=str(r["id"]), cart_id=str(r["cart_id"]),
                product_id=str(r["product_id"]), product_name=r["product_name"],
                price=Decimal(str(r["price"])), quantity=r["quantity"],
                created_at=r["created_at"],
            )
            for r in items_rows
        ]

        return Cart(
            id=str(row["id"]), user_id=str(row["user_id"]),
            status=row["status"], coupon_code=row["coupon_code"],
            discount_percent=Decimal(str(row["discount_percent"])),
            items=items, created_at=row["created_at"], updated_at=row["updated_at"],
        )

    async def find_by_user(self, user_id: str) -> Optional[Cart]:
        row = await self.db.fetch_one(
            "SELECT * FROM carts WHERE user_id = $1 AND status = 'ACTIVE'", user_id
        )
        if not row:
            return None
        return await self.find_by_id(str(row["id"]))

    async def add_item(self, item: CartItem) -> CartItem:
        query = """
            INSERT INTO cart_items (id, cart_id, product_id, product_name, price, quantity)
            VALUES ($1, $2, $3, $4, $5, $6) RETURNING *
        """
        row = await self.db.fetch_one(
            query, item.id, item.cart_id, item.product_id,
            item.product_name, item.price, item.quantity
        )
        return CartItem(
            id=str(row["id"]), cart_id=str(row["cart_id"]),
            product_id=str(row["product_id"]), product_name=row["product_name"],
            price=Decimal(str(row["price"])), quantity=row["quantity"],
            created_at=row["created_at"],
        )

    async def update_item_quantity(self, item_id: str, quantity: int) -> None:
        await self.db.execute(
            "UPDATE cart_items SET quantity = $2 WHERE id = $1", item_id, quantity
        )

    async def remove_item(self, item_id: str) -> bool:
        result = await self.db.execute("DELETE FROM cart_items WHERE id = $1", item_id)
        return result == "DELETE 1"

    async def apply_coupon(self, cart_id: str, coupon_code: str, discount: float) -> None:
        await self.db.execute(
            "UPDATE carts SET coupon_code = $2, discount_percent = $3, updated_at = NOW() WHERE id = $1",
            cart_id, coupon_code, discount
        )

    async def clear_cart(self, cart_id: str) -> None:
        await self.db.execute("DELETE FROM cart_items WHERE cart_id = $1", cart_id)

    async def update_status(self, cart_id: str, status: str) -> None:
        await self.db.execute(
            "UPDATE carts SET status = $2, updated_at = NOW() WHERE id = $1", cart_id, status
        )
