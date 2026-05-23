import hashlib
from typing import Optional

from app.domain.models import User, Address
from app.domain.interfaces import UserRepositoryProtocol, EventProducerProtocol


class UserService:
    def __init__(self, repository: UserRepositoryProtocol, producer: EventProducerProtocol):
        self.repository = repository
        self.producer = producer

    async def create_user(self, email: str, password: str, first_name: str,
                          last_name: str, phone: Optional[str] = None) -> User:
        existing = await self.repository.find_by_email(email)
        if existing:
            raise ValueError(f"User with email {email} already exists")

        user = User(
            email=email,
            password_hash=self._hash_password(password),
            first_name=first_name,
            last_name=last_name,
            phone=phone,
        )
        created = await self.repository.create(user)

        await self.producer.publish(
            topic="user-events",
            key=created.id,
            value={"event": "USER_CREATED", "userId": created.id, "email": created.email},
        )
        return created

    async def get_user(self, user_id: str) -> Optional[User]:
        return await self.repository.find_by_id(user_id)

    async def get_user_by_email(self, email: str) -> Optional[User]:
        return await self.repository.find_by_email(email)

    async def list_users(self, limit: int = 50, offset: int = 0) -> list[User]:
        return await self.repository.find_all(limit, offset)

    async def update_user(self, user_id: str, **kwargs) -> Optional[User]:
        user = await self.repository.find_by_id(user_id)
        if not user:
            return None

        for key, value in kwargs.items():
            if value is not None and hasattr(user, key):
                setattr(user, key, value)

        updated = await self.repository.update(user)

        await self.producer.publish(
            topic="user-events",
            key=updated.id,
            value={"event": "USER_UPDATED", "userId": updated.id},
        )
        return updated

    async def delete_user(self, user_id: str) -> bool:
        result = await self.repository.delete(user_id)
        if result:
            await self.producer.publish(
                topic="user-events",
                key=user_id,
                value={"event": "USER_DELETED", "userId": user_id},
            )
        return result

    async def add_address(self, user_id: str, street: str, city: str,
                          state: str, zip_code: str, country: str,
                          is_default: bool = False) -> Address:
        user = await self.repository.find_by_id(user_id)
        if not user:
            raise ValueError(f"User {user_id} not found")

        address = Address(
            user_id=user_id,
            street=street,
            city=city,
            state=state,
            zip_code=zip_code,
            country=country,
            is_default=is_default,
        )
        return await self.repository.create_address(address)

    async def get_addresses(self, user_id: str) -> list[Address]:
        return await self.repository.find_addresses_by_user(user_id)

    async def delete_address(self, address_id: str) -> bool:
        return await self.repository.delete_address(address_id)

    @staticmethod
    def _hash_password(password: str) -> str:
        return hashlib.sha256(password.encode()).hexdigest()
