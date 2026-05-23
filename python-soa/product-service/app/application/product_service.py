"""
Product Service Implementation
================================
Concrete implementation of IProductService protocol.
Orchestrates repository operations and event publishing.

Design Pattern: Service Layer Implementation
- Coordinates between persistence and messaging
- Contains application-level business rules
- Publishes domain events for inter-service communication

SOLID Principle: Single Responsibility (SRP)
- This class only handles product-related business operations
- Persistence is delegated to the repository
- Event publishing is delegated to the producer
"""

import logging
from decimal import Decimal
from uuid import UUID, uuid4

from app.domain.model import Product
from app.infrastructure.persistence.product_repository import ProductRepository
from app.infrastructure.messaging.kafka_producer import KafkaEventPublisher
from app.infrastructure.messaging.events import ProductEvents

logger = logging.getLogger(__name__)


class ProductServiceImpl:
    """
    Implementation of the Product Service.

    This class satisfies the IProductService protocol through structural typing.
    It coordinates between the repository (data access) and the event publisher
    (inter-service communication).
    """

    def __init__(self, repository: ProductRepository, publisher: KafkaEventPublisher):
        self._repository = repository
        self._publisher = publisher

    async def create_product(
        self,
        name: str,
        description: str,
        price: float,
        category: str,
        sku: str,
    ) -> Product:
        """
        Create a new product and publish a 'product.created' event.

        The event allows other services (e.g., Inventory Service) to react
        to new products being added to the catalog.
        """
        product = Product(
            id=uuid4(),
            name=name,
            description=description,
            price=Decimal(str(price)),
            category=category,
            sku=sku,
        )

        # Persist to database
        await self._repository.save(product)
        logger.info("Product created: %s (SKU: %s)", product.name, product.sku)

        # Publish event for other services
        event = ProductEvents.product_created(product)
        await self._publisher.publish("product.created", str(product.id), event)
        logger.info("Published product.created event for %s", product.id)

        return product

    async def get_by_id(self, product_id: UUID) -> Product | None:
        """Retrieve a product by ID from the repository."""
        return await self._repository.find_by_id(product_id)

    async def get_all(self) -> list[Product]:
        """Retrieve all active products."""
        return await self._repository.find_all()

    async def update_product(
        self,
        product_id: UUID,
        name: str | None = None,
        description: str | None = None,
        price: float | None = None,
        category: str | None = None,
    ) -> Product | None:
        """
        Update product attributes.

        Only non-None fields are updated, following the partial update pattern.
        """
        product = await self._repository.find_by_id(product_id)
        if not product:
            return None

        if name is not None:
            product.name = name
        if description is not None:
            product.description = description
        if price is not None:
            product.update_price(Decimal(str(price)))
        if category is not None:
            product.category = category

        await self._repository.update(product)
        logger.info("Product updated: %s", product_id)
        return product

    async def delete_product(self, product_id: UUID) -> bool:
        """
        Soft-delete a product by deactivating it.

        Products are never hard-deleted to maintain referential integrity
        across the distributed system.
        """
        product = await self._repository.find_by_id(product_id)
        if not product:
            return False

        product.deactivate()
        await self._repository.update(product)
        logger.info("Product deactivated: %s", product_id)
        return True
