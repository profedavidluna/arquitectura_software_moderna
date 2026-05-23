from fastapi import APIRouter, HTTPException, Request, Query

from app.infrastructure.web.dto import (
    CreateProductRequest, UpdateProductRequest, ProductResponse,
    CreateCategoryRequest, CategoryResponse,
    CreateReviewRequest, ReviewResponse,
)


def create_router() -> APIRouter:
    router = APIRouter()

    @router.post("", response_model=ProductResponse, status_code=201)
    async def create_product(request: Request, body: CreateProductRequest):
        service = request.app.state.service
        product = await service.create_product(
            name=body.name, description=body.description,
            price=body.price, category_id=body.category_id,
            image_url=body.image_url,
        )
        return ProductResponse(
            id=product.id, name=product.name, description=product.description,
            price=product.price, category_id=product.category_id,
            image_url=product.image_url, status=product.status,
            created_at=product.created_at, updated_at=product.updated_at,
        )

    @router.get("", response_model=list[ProductResponse])
    async def list_products(
        request: Request,
        limit: int = Query(default=50, ge=1, le=100),
        offset: int = Query(default=0, ge=0),
    ):
        service = request.app.state.service
        products = await service.list_products(limit, offset)
        return [
            ProductResponse(
                id=p.id, name=p.name, description=p.description,
                price=p.price, category_id=p.category_id,
                image_url=p.image_url, status=p.status,
                created_at=p.created_at, updated_at=p.updated_at,
            )
            for p in products
        ]

    @router.get("/search", response_model=list[ProductResponse])
    async def search_products(request: Request, q: str = Query(..., min_length=1)):
        service = request.app.state.service
        products = await service.search_products(q)
        return [
            ProductResponse(
                id=p.id, name=p.name, description=p.description,
                price=p.price, category_id=p.category_id,
                image_url=p.image_url, status=p.status,
                created_at=p.created_at, updated_at=p.updated_at,
            )
            for p in products
        ]

    @router.get("/categories", response_model=list[CategoryResponse])
    async def list_categories(request: Request):
        service = request.app.state.service
        categories = await service.list_categories()
        return [
            CategoryResponse(
                id=c.id, name=c.name, description=c.description,
                parent_id=c.parent_id, created_at=c.created_at,
            )
            for c in categories
        ]

    @router.post("/categories", response_model=CategoryResponse, status_code=201)
    async def create_category(request: Request, body: CreateCategoryRequest):
        service = request.app.state.service
        category = await service.create_category(
            name=body.name, description=body.description, parent_id=body.parent_id,
        )
        return CategoryResponse(
            id=category.id, name=category.name, description=category.description,
            parent_id=category.parent_id, created_at=category.created_at,
        )

    @router.get("/{product_id}", response_model=ProductResponse)
    async def get_product(request: Request, product_id: str):
        service = request.app.state.service
        product = await service.get_product(product_id)
        if not product:
            raise HTTPException(status_code=404, detail="Product not found")
        return ProductResponse(
            id=product.id, name=product.name, description=product.description,
            price=product.price, category_id=product.category_id,
            image_url=product.image_url, status=product.status,
            created_at=product.created_at, updated_at=product.updated_at,
        )

    @router.put("/{product_id}", response_model=ProductResponse)
    async def update_product(request: Request, product_id: str, body: UpdateProductRequest):
        service = request.app.state.service
        product = await service.update_product(product_id, **body.model_dump(exclude_none=True))
        if not product:
            raise HTTPException(status_code=404, detail="Product not found")
        return ProductResponse(
            id=product.id, name=product.name, description=product.description,
            price=product.price, category_id=product.category_id,
            image_url=product.image_url, status=product.status,
            created_at=product.created_at, updated_at=product.updated_at,
        )

    @router.delete("/{product_id}", status_code=204)
    async def delete_product(request: Request, product_id: str):
        service = request.app.state.service
        deleted = await service.delete_product(product_id)
        if not deleted:
            raise HTTPException(status_code=404, detail="Product not found")

    @router.post("/{product_id}/reviews", response_model=ReviewResponse, status_code=201)
    async def add_review(request: Request, product_id: str, body: CreateReviewRequest):
        service = request.app.state.service
        review = await service.add_review(
            product_id=product_id, user_id=body.user_id,
            rating=body.rating, comment=body.comment,
        )
        return ReviewResponse(
            id=review.id, product_id=review.product_id,
            user_id=review.user_id, rating=review.rating,
            comment=review.comment, created_at=review.created_at,
        )

    @router.get("/{product_id}/reviews", response_model=list[ReviewResponse])
    async def get_reviews(request: Request, product_id: str):
        service = request.app.state.service
        reviews = await service.get_reviews(product_id)
        return [
            ReviewResponse(
                id=r.id, product_id=r.product_id,
                user_id=r.user_id, rating=r.rating,
                comment=r.comment, created_at=r.created_at,
            )
            for r in reviews
        ]

    return router
