from __future__ import annotations

from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):


    model_config = SettingsConfigDict(
        env_prefix="ANALYTICS_",
        env_file=".env",
        extra="ignore",
        protected_namespaces=(),
    )

    app_name: str = "feedback-analytics"
    version: str = "2.0.0"
    debug: bool = False


    api_key: str = ""


    model_dir: Path = Path("data")
    model_file: str = "category_classifier.joblib"


    min_category_confidence: float = 0.25


    max_text_length: int = 20_000

    @property
    def model_path(self) -> Path:
        return self.model_dir / self.model_file


@lru_cache
def get_settings() -> Settings:
    return Settings()
