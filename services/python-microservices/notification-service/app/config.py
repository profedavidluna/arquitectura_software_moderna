from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "notification-service"
    app_port: int = 7088
    kafka_bootstrap_servers: str = "localhost:9097"
    redis_url: str = "redis://localhost:6381"

    class Config:
        env_file = ".env"


settings = Settings()
