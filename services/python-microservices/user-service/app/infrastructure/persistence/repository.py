from typing import Optional
from app.domain.models import User, Address
from app.infrastructure.persistence.database import Database


class UserRepository:
    def __init__(self, db: Database):
        self.db = db

    async def create(self, user: User) -> User:
        query = """
            INSERT INTO users (id, email, password_hash, first_name, last_name, phone, status)
            VALUES ($1, $2, $3, $4, $5, $6, $7)
            RETURNING id, email, password_hash, first_name, last_name, phone, status, created_at, updated_at
        """
        row = await self.db.fetch_one(
            query, user.id, user.email, user.password_hash,
            user.first_name, user.last_name, user.phone, user.status
        )
        return self._row_to_user(row)

    async def find_by_id(self, user_id: str) -> Optional[User]:
        query = "SELECT * FROM users WHERE id = $1"
        row = await self.db.fetch_one(query, user_id)
        return self._row_to_user(row) if row else None

    async def find_by_email(self, email: str) -> Optional[User]:
        query = "SELECT * FROM users WHERE email = $1"
        row = await self.db.fetch_one(query, email)
        return self._row_to_user(row) if row else None

    async def find_all(self, limit: int = 50, offset: int = 0) -> list[User]:
        query = "SELECT * FROM users ORDER BY created_at DESC LIMIT $1 OFFSET $2"
        rows = await self.db.fetch_all(query, limit, offset)
        return [self._row_to_user(row) for row in rows]

    async def update(self, user: User) -> User:
        query = """
            UPDATE users SET email=$2, first_name=$3, last_name=$4, phone=$5,
            status=$6, updated_at=NOW()
            WHERE id=$1
            RETURNING id, email, password_hash, first_name, last_name, phone, status, created_at, updated_at
        """
        row = await self.db.fetch_one(
            query, user.id, user.email, user.first_name,
            user.last_name, user.phone, user.status
        )
        return self._row_to_user(row)

    async def delete(self, user_id: str) -> bool:
        result = await self.db.execute("DELETE FROM users WHERE id = $1", user_id)
        return result == "DELETE 1"

    async def create_address(self, address: Address) -> Address:
        query = """
            INSERT INTO addresses (id, user_id, street, city, state, zip_code, country, is_default)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
            RETURNING id, user_id, street, city, state, zip_code, country, is_default, created_at
        """
        row = await self.db.fetch_one(
            query, address.id, address.user_id, address.street,
            address.city, address.state, address.zip_code,
            address.country, address.is_default
        )
        return self._row_to_address(row)

    async def find_addresses_by_user(self, user_id: str) -> list[Address]:
        query = "SELECT * FROM addresses WHERE user_id = $1"
        rows = await self.db.fetch_all(query, user_id)
        return [self._row_to_address(row) for row in rows]

    async def delete_address(self, address_id: str) -> bool:
        result = await self.db.execute("DELETE FROM addresses WHERE id = $1", address_id)
        return result == "DELETE 1"

    @staticmethod
    def _row_to_user(row) -> User:
        return User(
            id=str(row["id"]),
            email=row["email"],
            password_hash=row["password_hash"],
            first_name=row["first_name"],
            last_name=row["last_name"],
            phone=row["phone"],
            status=row["status"],
            created_at=row["created_at"],
            updated_at=row["updated_at"],
        )

    @staticmethod
    def _row_to_address(row) -> Address:
        return Address(
            id=str(row["id"]),
            user_id=str(row["user_id"]),
            street=row["street"],
            city=row["city"],
            state=row["state"],
            zip_code=row["zip_code"],
            country=row["country"],
            is_default=row["is_default"],
            created_at=row["created_at"],
        )
