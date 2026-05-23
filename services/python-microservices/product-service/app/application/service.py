import json
from typing import Optional
from decimal import Decimal

from app.domain.models import Product, Category, ProductReview
from app.domain.interfaces import ProductRepositoryProtocol, EventProducerProtocol, CacheProtocol


class ProductService:
    def __init__(self, repository: ProductRepositoryProtocol,
                 producer: EventProducerProtocol, cache: CacheProtocol):
        self.repository = repository
        self.producer = producer
        self.cache = cache

    async def create_product(self, name: str, description: Optional[str],
                             price: Decimal, category_id: Optional[str] = None,
                             image_url: Optional[str] = None) -> Product:
        product = Product(
            name=name, description=description, price=price,
            category_id=category_id, image_url=image_url,
        )
        created = await self.repository.create(product)

        await self.producer.publish(
            topic="product-events",
            key=created.id,
            value={"event": "PRODUCT_CREATED", "productId": created.id, "name": created.name},
        )
        return created

    async def get_product(self, product_id: str) -> Optional[Product]:
        # Cache-aside pattern
        cache_key = f"product:{product_id}"
        cached = await self.cache.get(cache_key)
        if cached:
            data = json.loads(cached)
            return Product(**data)

        product = await self.repository.find_by_id(product_id)
        if product:
            await self.cache.set(cache_key, json.dumps({
                "id": product.id, "name": product.name,
                "description": product.description,
                "price": str(product.price), "category_id": product.category_id,
                "image_url": product.image_url, "status": product.status,
                "created_at": product.created_at.isoformat(),
                "updated_at": product.updated_at.isoformat(),
            }))
        return product

    async def list_products(self, limit: int = 50, offset: int = 0) -> list[Product]:
        return await self.repository.find_all(limit, offset)

    async def search_products(self, query: str) -> list[Product]:
        return await self.repository.search(query)

    async def get_products_by_category(self, category_id: str) -> list[Product]:
        return await self.repository.find_by_category(category_id)

    async def update_product(self, product_id: str, **kwargs) -> Optional[Product]:
        product = await self.repository.find_by_id(product_id)
        if not product:
            return None

        for key, value in kwargs.items():
            if value is not None and hasattr(product, key):
                setattr(product, key, value)

        updated = await self.repository.update(product)

        # Invalidate cache
        await self.cache.delete(f"product:{product_id}")

        await self.producer.publish(
            topic="product-events",
            key=updated.id,
            value={"event": "PRODUCT_UPDATED", "productId": updated.id},
        )
        return updated

    async def delete_product(self, product_id: str) -> bool:
        result = await self.repository.delete(product_id)
        if result:
            await self.cache.delete(f"product:{product_id}")
            await self.producer.publish(
                topic="product-events",
                key=product_id,
                value={"event": "PRODUCT_DELETED", "productId": product_id},
            )
        return result

    async def create_category(self, name: str, description: Optional[str] = None,
                              parent_id: Optional[str] = None) -> Category:
        category = Category(name=name, description=description, parent_id=parent_id)
        return await self.repository.create_category(category)

    async def list_categories(self) -> list[Category]:
        return await self.repository.find_all_categories()

    async def add_review(self, product_id: str, user_id: str,
                         rating: int, comment: Optional[str] = None) -> ProductReview:
        review = ProductReview(
            product_id=product_id, user_id=user_id,
            rating=rating, comment=comment,
        )
        created = await self.repository.create_review(review)

        await self.producer.publish(
            topic="product-events",
            key=product_id,
            value={"event": "REVIEW_ADDED", "productId": product_id, "rating": rating},
        )
        return created

    async def get_reviews(self, product_id: str) -> list[ProductReview]:
        return await self.repository.find_reviews_by_product(product_id)
