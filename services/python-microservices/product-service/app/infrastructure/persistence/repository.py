from typing import Optional
from decimal import Decimal
from app.domain.models import Product, Category, ProductReview
from app.infrastructure.persistence.database import Database


class ProductRepository:
    def __init__(self, db: Database):
        self.db = db

    async def create(self, product: Product) -> Product:
        query = """
            INSERT INTO products (id, name, description, price, category_id, image_url, status)
            VALUES ($1, $2, $3, $4, $5, $6, $7)
            RETURNING *
        """
        row = await self.db.fetch_one(
            query, product.id, product.name, product.description,
            product.price, product.category_id, product.image_url, product.status
        )
        return self._row_to_product(row)

    async def find_by_id(self, product_id: str) -> Optional[Product]:
        row = await self.db.fetch_one("SELECT * FROM products WHERE id = $1", product_id)
        return self._row_to_product(row) if row else None

    async def find_all(self, limit: int = 50, offset: int = 0) -> list[Product]:
        rows = await self.db.fetch_all(
            "SELECT * FROM products WHERE status = 'ACTIVE' ORDER BY created_at DESC LIMIT $1 OFFSET $2",
            limit, offset
        )
        return [self._row_to_product(row) for row in rows]

    async def find_by_category(self, category_id: str) -> list[Product]:
        rows = await self.db.fetch_all(
            "SELECT * FROM products WHERE category_id = $1 AND status = 'ACTIVE'", category_id
        )
        return [self._row_to_product(row) for row in rows]

    async def search(self, query: str) -> list[Product]:
        rows = await self.db.fetch_all(
            "SELECT * FROM products WHERE (name ILIKE $1 OR description ILIKE $1) AND status = 'ACTIVE'",
            f"%{query}%"
        )
        return [self._row_to_product(row) for row in rows]

    async def update(self, product: Product) -> Product:
        query = """
            UPDATE products SET name=$2, description=$3, price=$4, category_id=$5,
            image_url=$6, status=$7, updated_at=NOW()
            WHERE id=$1 RETURNING *
        """
        row = await self.db.fetch_one(
            query, product.id, product.name, product.description,
            product.price, product.category_id, product.image_url, product.status
        )
        return self._row_to_product(row)

    async def delete(self, product_id: str) -> bool:
        result = await self.db.execute(
            "UPDATE products SET status='DELETED' WHERE id = $1", product_id
        )
        return "UPDATE 1" in result

    async def create_category(self, category: Category) -> Category:
        query = """
            INSERT INTO categories (id, name, description, parent_id)
            VALUES ($1, $2, $3, $4) RETURNING *
        """
        row = await self.db.fetch_one(
            query, category.id, category.name, category.description, category.parent_id
        )
        return Category(
            id=str(row["id"]), name=row["name"],
            description=row["description"], parent_id=str(row["parent_id"]) if row["parent_id"] else None,
            created_at=row["created_at"],
        )

    async def find_all_categories(self) -> list[Category]:
        rows = await self.db.fetch_all("SELECT * FROM categories ORDER BY name")
        return [
            Category(
                id=str(row["id"]), name=row["name"],
                description=row["description"],
                parent_id=str(row["parent_id"]) if row["parent_id"] else None,
                created_at=row["created_at"],
            )
            for row in rows
        ]

    async def create_review(self, review: ProductReview) -> ProductReview:
        query = """
            INSERT INTO product_reviews (id, product_id, user_id, rating, comment)
            VALUES ($1, $2, $3, $4, $5) RETURNING *
        """
        row = await self.db.fetch_one(
            query, review.id, review.product_id, review.user_id, review.rating, review.comment
        )
        return ProductReview(
            id=str(row["id"]), product_id=str(row["product_id"]),
            user_id=str(row["user_id"]), rating=row["rating"],
            comment=row["comment"], created_at=row["created_at"],
        )

    async def find_reviews_by_product(self, product_id: str) -> list[ProductReview]:
        rows = await self.db.fetch_all(
            "SELECT * FROM product_reviews WHERE product_id = $1 ORDER BY created_at DESC", product_id
        )
        return [
            ProductReview(
                id=str(row["id"]), product_id=str(row["product_id"]),
                user_id=str(row["user_id"]), rating=row["rating"],
                comment=row["comment"], created_at=row["created_at"],
            )
            for row in rows
        ]

    @staticmethod
    def _row_to_product(row) -> Product:
        return Product(
            id=str(row["id"]), name=row["name"], description=row["description"],
            price=Decimal(str(row["price"])), category_id=str(row["category_id"]) if row["category_id"] else None,
            image_url=row["image_url"], status=row["status"],
            created_at=row["created_at"], updated_at=row["updated_at"],
        )
