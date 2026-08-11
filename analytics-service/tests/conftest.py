from __future__ import annotations

import os
import tempfile
from collections.abc import Iterator
from pathlib import Path

import pytest

_TEMP_MODEL_DIR = Path(tempfile.mkdtemp(prefix="feedback-analytics-tests-"))
os.environ["ANALYTICS_MODEL_DIR"] = str(_TEMP_MODEL_DIR)
os.environ["ANALYTICS_API_KEY"] = "test-api-key"

from fastapi.testclient import TestClient

from app.core.config import get_settings
from app.main import create_app
from app.services.classifier import classifier


@pytest.fixture(scope="session", autouse=True)
def trained_model() -> Iterator[None]:
    get_settings.cache_clear()
    classifier.load_or_train(_TEMP_MODEL_DIR / "test_model.joblib")
    yield


@pytest.fixture(scope="session")
def client(trained_model: None) -> Iterator[TestClient]:
    with TestClient(create_app()) as test_client:
        yield test_client


@pytest.fixture
def auth_headers() -> dict[str, str]:
    return {"X-API-Key": "test-api-key"}
