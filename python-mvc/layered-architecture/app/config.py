"""
Configuration module for the Layered Architecture application.

Reads database and application settings from environment variables.
In Layered Architecture, configuration is a cross-cutting concern
that can be accessed by any layer as needed.
"""
from __future__ import annotations

import os
from dataclasses import dataclass

from dotenv import load_dotenv

load_dotenv()


@dataclass(frozen=True)
class Settings:
    """Application settings loaded from environment variables."""

    db_host: str = os.environ.get("DB_HOST", "localhost")
    db_port: int = int(os.environ.get("DB_PORT", "5434"))
    db_user: str = os.environ.get("DB_USER", "postgres")
    db_password: str = os.environ.get("DB_PASSWORD", "postgres")
    db_name: str = os.environ.get("DB_NAME", "layered_db")
    use_postgres: bool = os.environ.get("USE_POSTGRES", "false").lower() == "true"

    @property
    def database_url(self) -> str:
        """Build the PostgreSQL connection DSN."""
        return (
            f"postgresql://{self.db_user}:{self.db_password}"
            f"@{self.db_host}:{self.db_port}/{self.db_name}"
        )


settings = Settings()
