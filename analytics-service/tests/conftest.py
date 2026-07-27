"""Shared fixtures.

The classifier is trained once per test session into a temporary directory, so
the suite never touches a developer's real model artifact and never pays the
training cost more than once.
"""

from __future__ import annotations

import os
import tempfile
from collections.abc import Iterator
from pathlib import Path

import pytest

# Point configuration at a scratch directory before anything imports it.
_TEMP_MODEL_DIR = Path(tempfile.mkdtemp(prefix="feedback-analytics-tests-"))
os.environ["ANALYTICS_MODEL_DIR"] = str(_TEMP_MODEL_DIR)
os.environ["ANALYTICS_API_KEY"] = "test-api-key"

from fastapi.testclient import TestClient  # noqa: E402

from app.core.config import get_settings  # noqa: E402
from app.main import create_app  # noqa: E402
from app.services.classifier import classifier  # noqa: E402


@pytest.fixture(scope="session", autouse=True)
def trained_model() -> Iterator[None]:
    """Train the classifier once for the whole session."""
    get_settings.cache_clear()
    classifier.load_or_train(_TEMP_MODEL_DIR / "test_model.joblib")
    yield


@pytest.fixture(scope="session")
def client(trained_model: None) -> Iterator[TestClient]:
    """A TestClient with the lifespan hook executed."""
    with TestClient(create_app()) as test_client:
        yield test_client


@pytest.fixture
def auth_headers() -> dict[str, str]:
    return {"X-API-Key": "test-api-key"}
