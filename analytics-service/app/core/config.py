"""Runtime configuration, read from the environment.

Nothing sensitive is defaulted in code. The API key in particular has no
usable default: an empty key disables authentication, which is only ever
appropriate for local development, and the service logs a warning when it
starts in that state.
"""

from __future__ import annotations

from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Environment-driven settings for the analytics service."""

    # protected_namespaces is cleared because several settings legitimately
    # start with "model_"; without this pydantic warns on every import.
    model_config = SettingsConfigDict(
        env_prefix="ANALYTICS_",
        env_file=".env",
        extra="ignore",
        protected_namespaces=(),
    )

    app_name: str = "feedback-analytics"
    version: str = "2.0.0"
    debug: bool = False

    # Shared secret expected in the X-API-Key header. Empty disables the check.
    api_key: str = ""

    # Where the fitted classifier is written and read from.
    model_dir: Path = Path("data")
    model_file: str = "category_classifier.joblib"

    # Minimum probability before a category suggestion is offered at all.
    min_category_confidence: float = 0.25

    # Requests longer than this are truncated before vectorisation.
    max_text_length: int = 20_000

    @property
    def model_path(self) -> Path:
        return self.model_dir / self.model_file


@lru_cache
def get_settings() -> Settings:
    """Cached accessor so the environment is read once per process."""
    return Settings()
