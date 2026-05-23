from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "payment-service"
    app_port: int = 7086
    database_url: str = "postgresql://postgres:postgres@localhost:5441/payment_db"
    kafka_bootstrap_servers: str = "localhost:9097"
    redis_url: str = "redis://localhost:6381"
    max_retries: int = 3
    retry_base_delay: float = 1.0

    class Config:
        env_file = ".env"


settings = Settings()
