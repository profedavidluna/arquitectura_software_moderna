from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "order-service"
    app_port: int = 7085
    database_url: str = "postgresql://postgres:postgres@localhost:5441/order_db"
    kafka_bootstrap_servers: str = "localhost:9097"
    redis_url: str = "redis://localhost:6381"
    cart_service_url: str = "http://localhost:7084"
    inventory_service_url: str = "http://localhost:7087"
    payment_service_url: str = "http://localhost:7086"

    class Config:
        env_file = ".env"


settings = Settings()
