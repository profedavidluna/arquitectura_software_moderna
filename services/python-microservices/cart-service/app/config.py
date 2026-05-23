from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "cart-service"
    app_port: int = 7084
    database_url: str = "postgresql://postgres:postgres@localhost:5441/cart_db"
    kafka_bootstrap_servers: str = "localhost:9097"
    redis_url: str = "redis://localhost:6381"
    product_service_url: str = "http://localhost:7083"
    cache_ttl: int = 300

    class Config:
        env_file = ".env"


settings = Settings()
