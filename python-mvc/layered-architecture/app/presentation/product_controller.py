"""
Presentation Layer: Product REST Controller.

FastAPI router that handles HTTP requests, validates input via DTOs,
delegates to the Business Layer, and maps errors to HTTP status codes.

LAYERED ARCHITECTURE FLOW:
    HTTP Request → Controller (Presentation) → Service (Business) → Repository (Data)
    HTTP Response ← Controller (Presentation) ← Service (Business) ← Repository (Data)

KEY DIFFERENCE FROM HEXAGONAL ARCHITECTURE:
- Hexagonal: The controller (input adapter) depends on an abstract input port.
  It never imports the concrete service. Error types come from the domain core.
- Layered: The controller directly imports and depends on the concrete
  ProductService class and its error types from the Business Layer.
  There is no indirection through ports/interfaces.

This direct dependency makes the code simpler to follow but means the
Presentation Layer is tightly coupled to the Business Layer's implementation.
"""
from __future__ import annotations

from fastapi import APIRouter, HTTPException, Query, status

from app.business.errors import (
    ConflictError,
    InsufficientStockError,
    NotFoundError,
    ValidationError,
)
from app.business.product_service import ProductService
from app.presentation.dto import (
    CreateProductRequest,
    PaginatedResponse,
    ProductResponse,
    UpdateProductRequest,
)

router = APIRouter(prefix="/api/v1/products", tags=["Products"])

# Service instance — wired at application startup
_product_service: ProductService | None = None


def set_product_service(service: ProductService) -> None:
    """Wire the product service into the controller at startup."""
    global _product_service
    _product_service = service


def _get_service() -> ProductService:
    """Get the product service instance."""
    if _product_service is None:
        raise RuntimeError("ProductService has not been initialized")
    return _product_service


def _to_response(product) -> ProductResponse:
    """Map a Product data model to a ProductResponse DTO."""
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
)
async def create_product(request: CreateProductRequest) -> ProductResponse:
    """Create a new product in the catalog."""
    try:
        product = await _get_service().create_product(
            name=request.name,
            description=request.description,
            price=request.price,
            category=request.category,
            stock_quantity=request.stock_quantity,
            sku=request.sku,
        )
        return _to_response(product)
    except ValidationError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, detail=str(e)
        )
    except ConflictError as e:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT, detail=str(e)
        )


@router.get("", response_model=PaginatedResponse)
async def list_products(
    page: int = Query(1, ge=1, description="Page number"),
    size: int = Query(10, ge=1, le=100, description="Page size"),
) -> PaginatedResponse:
    """List all products with pagination."""
    result = await _get_service().list_products(page=page, size=size)
    return PaginatedResponse(
        items=[_to_response(p) for p in result["items"]],
        total=result["total"],
        page=result["page"],
        size=result["size"],
        total_pages=result["total_pages"],
    )


@router.get("/search", response_model=list[ProductResponse])
async def search_products(
    query: str | None = Query(None, description="Search query for name/description"),
    category: str | None = Query(None, description="Filter by category"),
    min_price: float | None = Query(None, ge=0, description="Minimum price"),
    max_price: float | None = Query(None, ge=0, description="Maximum price"),
) -> list[ProductResponse]:
    """Search products by various criteria."""
    products = await _get_service().search_products(
        query=query,
        category=category,
        min_price=min_price,
        max_price=max_price,
    )
    return [_to_response(p) for p in products]


@router.get("/{product_id}", response_model=ProductResponse)
async def get_product(product_id: str) -> ProductResponse:
    """Get a product by its ID."""
    try:
        product = await _get_service().get_product_by_id(product_id)
        return _to_response(product)
    except NotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(e)
        )


@router.put("/{product_id}", response_model=ProductResponse)
async def update_product(
    product_id: str, request: UpdateProductRequest
) -> ProductResponse:
    """Update an existing product."""
    try:
        product = await _get_service().update_product(
            product_id=product_id,
            name=request.name,
            description=request.description,
            price=request.price,
            category=request.category,
            stock_quantity=request.stock_quantity,
            sku=request.sku,
            active=request.active,
        )
        return _to_response(product)
    except NotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(e)
        )
    except ValidationError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, detail=str(e)
        )
    except ConflictError as e:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT, detail=str(e)
        )


@router.delete("/{product_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_product(product_id: str) -> None:
    """Delete a product by its ID."""
    try:
        await _get_service().delete_product(product_id)
    except NotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(e)
        )


@router.patch("/{product_id}/stock/decrease", response_model=ProductResponse)
async def decrease_stock(
    product_id: str,
    quantity: int = Query(..., gt=0, description="Quantity to decrease"),
) -> ProductResponse:
    """Decrease the stock of a product."""
    try:
        product = await _get_service().decrease_stock(product_id, quantity)
        return _to_response(product)
    except NotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(e)
        )
    except (InsufficientStockError, ValidationError) as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, detail=str(e)
        )


@router.patch("/{product_id}/stock/increase", response_model=ProductResponse)
async def increase_stock(
    product_id: str,
    quantity: int = Query(..., gt=0, description="Quantity to increase"),
) -> ProductResponse:
    """Increase the stock of a product."""
    try:
        product = await _get_service().increase_stock(product_id, quantity)
        return _to_response(product)
    except NotFoundError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=str(e)
        )
    except ValidationError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, detail=str(e)
        )
