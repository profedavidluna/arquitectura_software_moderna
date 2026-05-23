"""
@layer Adapters (Interface Adapters) - Product Controller
@description FastAPI router that converts HTTP requests to use case calls.

In Clean Architecture, the controller:
1. Receives HTTP requests (from the frameworks layer)
2. Converts request data into use case input DTOs
3. Calls the appropriate use case
4. Converts use case output DTOs into HTTP responses

The controller depends on USE CASES (inner layer), not the other way around.
This maintains the Dependency Rule.
"""

from decimal import Decimal
from typing import Optional

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel, Field
from datetime import datetime

from app.usecases.create_product import CreateProductUseCase
from app.usecases.get_product import GetProductUseCase
from app.usecases.list_products import ListProductsUseCase
from app.usecases.search_products import SearchProductsUseCase
from app.usecases.update_product import UpdateProductUseCase
from app.usecases.delete_product import DeleteProductUseCase
from app.usecases.manage_stock import DecreaseStockUseCase, IncreaseStockUseCase
from app.usecases.dto import CreateProductInput, UpdateProductInput, ProductOutput
from app.usecases.errors import ProductNotFoundError, DuplicateSkuError, UseCaseError


# ============================================================
# Pydantic models for HTTP request/response (framework concern)
# ============================================================

class CreateProductRequest(BaseModel):
    """HTTP request body for creating a product."""
    name: str = Field(..., max_length=255)
    description: str = Field(default="")
    price: Decimal = Field(..., gt=0)
    category: str = Field(default="", max_length=100)
    stock_quantity: int = Field(default=0, ge=0)
    sku: str = Field(..., max_length=100)


class UpdateProductRequest(BaseModel):
    """HTTP request body for updating a product."""
    name: Optional[str] = Field(None, max_length=255)
    description: Optional[str] = None
    price: Optional[Decimal] = Field(None, gt=0)
    category: Optional[str] = Field(None, max_length=100)
    stock_quantity: Optional[int] = Field(None, ge=0)
    sku: Optional[str] = Field(None, max_length=100)


class ProductResponse(BaseModel):
    """HTTP response for a product."""
    id: str
    name: str
    description: str
    price: Decimal
    category: str
    stock_quantity: int
    sku: str
    active: bool
    created_at: datetime
    updated_at: datetime


class ProductListResponse(BaseModel):
    """HTTP response for paginated product list."""
    products: list[ProductResponse]
    total: int
    page: int
    size: int


# ============================================================
# Router and use case references
# ============================================================

router = APIRouter(prefix="/api/v1/products", tags=["Products"])

# Use cases will be injected from main.py
_create_product: Optional[CreateProductUseCase] = None
_get_product: Optional[GetProductUseCase] = None
_list_products: Optional[ListProductsUseCase] = None
_search_products: Optional[SearchProductsUseCase] = None
_update_product: Optional[UpdateProductUseCase] = None
_delete_product: Optional[DeleteProductUseCase] = None
_decrease_stock: Optional[DecreaseStockUseCase] = None
_increase_stock: Optional[IncreaseStockUseCase] = None


def set_use_cases(
    create: CreateProductUseCase,
    get: GetProductUseCase,
    list_all: ListProductsUseCase,
    search: SearchProductsUseCase,
    update: UpdateProductUseCase,
    delete: DeleteProductUseCase,
    decrease: DecreaseStockUseCase,
    increase: IncreaseStockUseCase,
) -> None:
    """Inject all use cases into the controller. Called from main.py."""
    global _create_product, _get_product, _list_products, _search_products
    global _update_product, _delete_product, _decrease_stock, _increase_stock
    _create_product = create
    _get_product = get
    _list_products = list_all
    _search_products = search
    _update_product = update
    _delete_product = delete
    _decrease_stock = decrease
    _increase_stock = increase


def _output_to_response(output: ProductOutput) -> ProductResponse:
    """Convert use case output DTO to HTTP response."""
    return ProductResponse(
        id=output.id,
        name=output.name,
        description=output.description,
        price=output.price,
        category=output.category,
        stock_quantity=output.stock_quantity,
        sku=output.sku,
        active=output.active,
        created_at=output.created_at,
        updated_at=output.updated_at,
    )


# ============================================================
# Route handlers
# ============================================================

@router.post("", response_model=ProductResponse, status_code=201)
async def create_product(request: CreateProductRequest):
    """Create a new product in the catalog."""
    try:
        input_data = CreateProductInput(
            name=request.name,
            description=request.description,
            price=request.price,
            category=request.category,
            stock_quantity=request.stock_quantity,
            sku=request.sku,
        )
        output = await _create_product.execute(input_data)
        return _output_to_response(output)
    except (DuplicateSkuError, ValueError) as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("", response_model=ProductListResponse)
async def list_products(
    page: int = Query(1, ge=1),
    size: int = Query(10, ge=1, le=100),
):
    """List all active products with pagination."""
    output = await _list_products.execute(page, size)
    return ProductListResponse(
        products=[_output_to_response(p) for p in output.products],
        total=output.total,
        page=output.page,
        size=output.size,
    )


@router.get("/search", response_model=list[ProductResponse])
async def search_products(
    query: Optional[str] = Query(None),
    category: Optional[str] = Query(None),
    min_price: Optional[Decimal] = Query(None, ge=0),
    max_price: Optional[Decimal] = Query(None, ge=0),
):
    """Search products by various criteria."""
    outputs = await _search_products.execute(query, category, min_price, max_price)
    return [_output_to_response(o) for o in outputs]


@router.get("/{product_id}", response_model=ProductResponse)
async def get_product(product_id: str):
    """Get a product by its ID."""
    try:
        output = await _get_product.execute(product_id)
        return _output_to_response(output)
    except ProductNotFoundError:
        raise HTTPException(status_code=404, detail="Product not found")


@router.put("/{product_id}", response_model=ProductResponse)
async def update_product(product_id: str, request: UpdateProductRequest):
    """Update an existing product."""
    try:
        input_data = UpdateProductInput(
            product_id=product_id,
            name=request.name,
            description=request.description,
            price=request.price,
            category=request.category,
            stock_quantity=request.stock_quantity,
            sku=request.sku,
        )
        output = await _update_product.execute(input_data)
        return _output_to_response(output)
    except ProductNotFoundError:
        raise HTTPException(status_code=404, detail="Product not found")
    except (DuplicateSkuError, ValueError) as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{product_id}", status_code=204)
async def delete_product(product_id: str):
    """Soft-delete a product (sets active=False)."""
    try:
        await _delete_product.execute(product_id)
        return None
    except ProductNotFoundError:
        raise HTTPException(status_code=404, detail="Product not found")


@router.patch("/{product_id}/stock/decrease", response_model=ProductResponse)
async def decrease_stock(
    product_id: str,
    quantity: int = Query(..., gt=0),
):
    """Decrease product stock."""
    try:
        output = await _decrease_stock.execute(product_id, quantity)
        return _output_to_response(output)
    except ProductNotFoundError:
        raise HTTPException(status_code=404, detail="Product not found")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.patch("/{product_id}/stock/increase", response_model=ProductResponse)
async def increase_stock(
    product_id: str,
    quantity: int = Query(..., gt=0),
):
    """Increase product stock."""
    try:
        output = await _increase_stock.execute(product_id, quantity)
        return _output_to_response(output)
    except ProductNotFoundError:
        raise HTTPException(status_code=404, detail="Product not found")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
