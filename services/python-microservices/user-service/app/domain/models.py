from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional
import uuid


@dataclass
class User:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    email: str = ""
    password_hash: str = ""
    first_name: str = ""
    last_name: str = ""
    phone: Optional[str] = None
    status: str = "ACTIVE"
    created_at: datetime = field(default_factory=datetime.now)
    updated_at: datetime = field(default_factory=datetime.now)


@dataclass
class Address:
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    user_id: str = ""
    street: str = ""
    city: str = ""
    state: str = ""
    zip_code: str = ""
    country: str = ""
    is_default: bool = False
    created_at: datetime = field(default_factory=datetime.now)
