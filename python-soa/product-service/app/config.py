"""
Configuration Module
====================
Centralized configuration using Pydantic Settings.
Values are loaded from environment variables, enabling
12-factor app compliance and easy container orchestration.

SOA Principle: Service Discoverability
- Configuration externalizes connection details
- Services discover infrastructure through environment
"""

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    port: int = 9091
    db_host: str = "localhost"
    db_port: int = 5438
    db_user: str = "postgres"
    db_password: str = "postgres"
    db_name: str = "product_db"
    kafka_brokers: str = "localhost:9094"

    class Config:
        env_prefix = ""


settings = Settings()
