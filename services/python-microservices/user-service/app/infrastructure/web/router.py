from fastapi import APIRouter, HTTPException, Request, Query

from app.infrastructure.web.dto import (
    CreateUserRequest, UpdateUserRequest, UserResponse,
    CreateAddressRequest, AddressResponse,
)


def create_router() -> APIRouter:
    router = APIRouter()

    @router.post("", response_model=UserResponse, status_code=201)
    async def create_user(request: Request, body: CreateUserRequest):
        service = request.app.state.service
        try:
            user = await service.create_user(
                email=body.email,
                password=body.password,
                first_name=body.first_name,
                last_name=body.last_name,
                phone=body.phone,
            )
            return UserResponse(
                id=user.id, email=user.email, first_name=user.first_name,
                last_name=user.last_name, phone=user.phone, status=user.status,
                created_at=user.created_at, updated_at=user.updated_at,
            )
        except ValueError as e:
            raise HTTPException(status_code=409, detail=str(e))

    @router.get("", response_model=list[UserResponse])
    async def list_users(
        request: Request,
        limit: int = Query(default=50, ge=1, le=100),
        offset: int = Query(default=0, ge=0),
    ):
        service = request.app.state.service
        users = await service.list_users(limit, offset)
        return [
            UserResponse(
                id=u.id, email=u.email, first_name=u.first_name,
                last_name=u.last_name, phone=u.phone, status=u.status,
                created_at=u.created_at, updated_at=u.updated_at,
            )
            for u in users
        ]

    @router.get("/{user_id}", response_model=UserResponse)
    async def get_user(request: Request, user_id: str):
        service = request.app.state.service
        user = await service.get_user(user_id)
        if not user:
            raise HTTPException(status_code=404, detail="User not found")
        return UserResponse(
            id=user.id, email=user.email, first_name=user.first_name,
            last_name=user.last_name, phone=user.phone, status=user.status,
            created_at=user.created_at, updated_at=user.updated_at,
        )

    @router.put("/{user_id}", response_model=UserResponse)
    async def update_user(request: Request, user_id: str, body: UpdateUserRequest):
        service = request.app.state.service
        user = await service.update_user(user_id, **body.model_dump(exclude_none=True))
        if not user:
            raise HTTPException(status_code=404, detail="User not found")
        return UserResponse(
            id=user.id, email=user.email, first_name=user.first_name,
            last_name=user.last_name, phone=user.phone, status=user.status,
            created_at=user.created_at, updated_at=user.updated_at,
        )

    @router.delete("/{user_id}", status_code=204)
    async def delete_user(request: Request, user_id: str):
        service = request.app.state.service
        deleted = await service.delete_user(user_id)
        if not deleted:
            raise HTTPException(status_code=404, detail="User not found")

    @router.post("/{user_id}/addresses", response_model=AddressResponse, status_code=201)
    async def add_address(request: Request, user_id: str, body: CreateAddressRequest):
        service = request.app.state.service
        try:
            address = await service.add_address(
                user_id=user_id, street=body.street, city=body.city,
                state=body.state, zip_code=body.zip_code,
                country=body.country, is_default=body.is_default,
            )
            return AddressResponse(
                id=address.id, user_id=address.user_id, street=address.street,
                city=address.city, state=address.state, zip_code=address.zip_code,
                country=address.country, is_default=address.is_default,
                created_at=address.created_at,
            )
        except ValueError as e:
            raise HTTPException(status_code=404, detail=str(e))

    @router.get("/{user_id}/addresses", response_model=list[AddressResponse])
    async def get_addresses(request: Request, user_id: str):
        service = request.app.state.service
        addresses = await service.get_addresses(user_id)
        return [
            AddressResponse(
                id=a.id, user_id=a.user_id, street=a.street,
                city=a.city, state=a.state, zip_code=a.zip_code,
                country=a.country, is_default=a.is_default,
                created_at=a.created_at,
            )
            for a in addresses
        ]

    @router.delete("/{user_id}/addresses/{address_id}", status_code=204)
    async def delete_address(request: Request, user_id: str, address_id: str):
        service = request.app.state.service
        deleted = await service.delete_address(address_id)
        if not deleted:
            raise HTTPException(status_code=404, detail="Address not found")

    return router
