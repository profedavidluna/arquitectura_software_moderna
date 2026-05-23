from pydantic import BaseModel, EmailStr
from typing import Optional
from datetime import datetime


class CreateUserRequest(BaseModel):
    email: str
    password: str
    first_name: str
    last_name: str
    phone: Optional[str] = None


class UpdateUserRequest(BaseModel):
    email: Optional[str] = None
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    phone: Optional[str] = None
    status: Optional[str] = None


class UserResponse(BaseModel):
    id: str
    email: str
    first_name: str
    last_name: str
    phone: Optional[str] = None
    status: str
    created_at: datetime
    updated_at: datetime


class CreateAddressRequest(BaseModel):
    street: str
    city: str
    state: str
    zip_code: str
    country: str
    is_default: bool = False


class AddressResponse(BaseModel):
    id: str
    user_id: str
    street: str
    city: str
    state: str
    zip_code: str
    country: str
    is_default: bool
    created_at: datetime
