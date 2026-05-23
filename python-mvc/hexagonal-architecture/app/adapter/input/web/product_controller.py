"""
Input Adapter: Product REST Controller.

This is the driving/primary adapter that translates HTTP requests into
domain operations via the input port (ProductServicePort).

In Hexagonal Architecture, input adapters:
- Receive external requests (HTTP, CLI, messages)
- Translate them into domain commands/queries
- Call the input port (use case interface)
- Translate domain responses back to external format

This controller depends on the ProductServicePort abstraction,
NOT on the concrete ProductService implementation.
"""
from __future__ import annotations

from fastapi import APIRouter, HTTPException, Query, status

from app.adapter.input.web.dto import (
    CreateProductRequest,
    ErrorResponse,
    PaginatedResponse,
    ProductResponse,
    UpdateProductRequest,
)
from app.domain.model.product import (
    DuplicateSkuError,
    InsufficientStockError,
    InvalidPriceError,
    InvalidStockError,
    ProductNotFoundError,
)
from app.domain.port.input.product_service_port import (
    CreateProductCommand,
    ProductServicePort,
    SearchCriteria,
    UpdateProductCommand,
)

router = APIRouter(prefix="/api/v1/products", tags=["Products"])

# This will be injected at application startup
_product_service: ProductServicePort | None = None


def set_product_service(service: ProductServicePort) -> None:
    """Wire the product service into the controller."""
    global _product_service
    _product_service = service


def get_service() -> ProductServicePort:
    """Get the product service instance."""
    if _product_service is None:
        raise RuntimeError("ProductService has not been initialized")
    return _product_service


def _to_response(product) -> ProductResponse:
    """Map a domain Product to a ProductResponse DTO."""
    return ProductResponse(
        id=product.id,
        name=product.name,
        description=product.description,
        price=product.price,
        category=product.category,
        stock_quantity=product.stock_quantity,
        sku=product.sku,
        active=product.active,
        created_at=product.created_at,
        updated_at=product.updated_at,
    )


@router.post(
    "",
    response_model=ProductResponse,
    status_code=status.HTTP_201_CREATED,
    responses={
        400: {"model": ErrorResponse},
        409: {"model": ErrorResponse},
    },
)
async def create_product(request: CreateProductRequest) -> ProductResponse:
    """Create a new product in the catalog."""
    try:
        command = CreateProductCommand(
            name=request.name,
            description=request.description,
            price=request.price,
            category=request.category,
            stock_quantity=request.stock_quantity,
            sku=request.sku,
        )
        product = await get_service().create_product(command)
        return _to_response(product)
    except (InvalidPriceError, InvalidStockError) as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, detail=str(e)
        )
    except DuplicateSkuError as e:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT, detail=str(e)
        )


@router.get("", response_model=PaginatedResponse)
async def list_products(
    page: int = Query(1, ge=1, description="Page number"),
    size: int = Query(10, ge=1, le=100, description="Page size"),
) -> PaginatedResponse:
    """List all products with pagination."""
    result = await get_service().list_products(page=page, size=size)
    return PaginatedResponse(
        items=[_to_response(p) for p in result.items],
        total=result.total,
        page=result.page,
        size=result.size,
        total_pages=result.total_pages,
    )


@router.get("/search", response_model=list[ProductResponse])
async def search_products(
    query: str | None = Query(None, description="Search query for name/description"),
    category: str | None = Query(None, description="Filter by category"),
    min_price: float | None = Query(None, ge=0, description="Minimum price"),
    max_price: float | None = Query(None, ge=0, description="Maximum price"),
) -> list[ProductResponse]:
    """Search products by various criteria."""
    criteria = SearchCriteria(
        query=query,
        category=category,
        min_price=min_price,
        max_price=max_price,
    )
    products = await get_service().search_products(criteria)
    return [_to_response(p) for p in products]


@router.get(
    "/{product_id}",
    response_model=ProductResponse,
    responses={404: {"model": ErrorResponse}},
)
async def get_product(product_id: str) -> ProductResponse:
    """Get a product by its ID."""
    try:
        product = await get_service().get_product_by_id(product_id)
        return _to_response(product)
    except ProductNotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(e)
        )


@router.put(
    "/{product_id}",
    response_model=ProductResponse,
    responses={
        400: {"model": ErrorResponse},
        404: {"model": ErrorResponse},
        409: {"model": ErrorResponse},
    },
)
async def update_product(
    product_id: str, request: UpdateProductRequest
) -> ProductResponse:
    """Update an existing product."""
    try:
        command = UpdateProductCommand(
            name=request.name,
            description=request.description,
            price=request.price,
            category=request.category,
            stock_quantity=request.stock_quantity,
            sku=request.sku,
            active=request.active,
        )
        product = await get_service().update_product(product_id, command)
        return _to_response(product)
    except ProductNotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(e)
        )
    except (InvalidPriceError, InvalidStockError) as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, detail=str(e)
        )
    except DuplicateSkuError as e:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT, detail=str(e)
        )


@router.delete(
    "/{product_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    responses={404: {"model": ErrorResponse}},
)
async def delete_product(product_id: str) -> None:
    """Delete a product by its ID."""
    try:
        await get_service().delete_product(product_id)
    except ProductNotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(e)
        )


@router.patch(
    "/{product_id}/stock/decrease",
    response_model=ProductResponse,
    responses={
        400: {"model": ErrorResponse},
        404: {"model": ErrorResponse},
    },
)
async def decrease_stock(
    product_id: str,
    quantity: int = Query(..., gt=0, description="Quantity to decrease"),
) -> ProductResponse:
    """Decrease the stock of a product."""
    try:
        product = await get_service().decrease_stock(product_id, quantity)
        return _to_response(product)
    except ProductNotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(e)
        )
    except (InsufficientStockError, InvalidStockError) as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, detail=str(e)
        )


@router.patch(
    "/{product_id}/stock/increase",
    response_model=ProductResponse,
    responses={
        400: {"model": ErrorResponse},
        404: {"model": ErrorResponse},
    },
)
async def increase_stock(
    product_id: str,
    quantity: int = Query(..., gt=0, description="Quantity to increase"),
) -> ProductResponse:
    """Increase the stock of a product."""
    try:
        product = await get_service().increase_stock(product_id, quantity)
        return _to_response(product)
    except ProductNotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(e)
        )
    except InvalidStockError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, detail=str(e)
        )
