from pydantic import BaseModel
from typing import Optional
from datetime import datetime
from decimal import Decimal


class CreateProductRequest(BaseModel):
    name: str
    description: Optional[str] = None
    price: Decimal
    category_id: Optional[str] = None
    image_url: Optional[str] = None


class UpdateProductRequest(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    price: Optional[Decimal] = None
    category_id: Optional[str] = None
    image_url: Optional[str] = None
    status: Optional[str] = None


class ProductResponse(BaseModel):
    id: str
    name: str
    description: Optional[str] = None
    price: Decimal
    category_id: Optional[str] = None
    image_url: Optional[str] = None
    status: str
    created_at: datetime
    updated_at: datetime


class CreateCategoryRequest(BaseModel):
    name: str
    description: Optional[str] = None
    parent_id: Optional[str] = None


class CategoryResponse(BaseModel):
    id: str
    name: str
    description: Optional[str] = None
    parent_id: Optional[str] = None
    created_at: datetime


class CreateReviewRequest(BaseModel):
    user_id: str
    rating: int
    comment: Optional[str] = None


class ReviewResponse(BaseModel):
    id: str
    product_id: str
    user_id: str
    rating: int
    comment: Optional[str] = None
    created_at: datetime
