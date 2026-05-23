from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "product-service"
    app_port: int = 7083
    database_url: str = "postgresql://postgres:postgres@localhost:5441/product_db"
    kafka_bootstrap_servers: str = "localhost:9097"
    redis_url: str = "redis://localhost:6381"
    cache_ttl: int = 300  # 5 minutes

    class Config:
        env_file = ".env"


settings = Settings()
