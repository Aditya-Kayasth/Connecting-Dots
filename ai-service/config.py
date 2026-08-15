from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    xai_api_key: str = "test-key"
    redis_url: str = "redis://localhost:6379/0"

    class Config:
        env_file = ".env"

settings = Settings()
