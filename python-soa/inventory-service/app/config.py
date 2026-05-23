"""
Configuration Module - Inventory Service
==========================================
"""

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    port: int = 9093
    db_host: str = "localhost"
    db_port: int = 5438
    db_user: str = "postgres"
    db_password: str = "postgres"
    db_name: str = "inventory_db"
    kafka_brokers: str = "localhost:9094"

    class Config:
        env_prefix = ""


settings = Settings()
