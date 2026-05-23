from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "user-service"
    app_port: int = 7082
    database_url: str = "postgresql://postgres:postgres@localhost:5441/user_db"
    kafka_bootstrap_servers: str = "localhost:9097"
    redis_url: str = "redis://localhost:6381"

    class Config:
        env_file = ".env"


settings = Settings()
