"""API-key authentication for service-to-service calls."""

from __future__ import annotations

import hmac
import logging

from fastapi import Header, HTTPException, status

from app.core.config import get_settings

logger = logging.getLogger(__name__)


async def require_api_key(x_api_key: str | None = Header(default=None)) -> None:
    """Validate the ``X-API-Key`` header.

    Comparison uses :func:`hmac.compare_digest` rather than ``==`` so the
    check does not leak the key one character at a time through response
    timing.

    Args:
        x_api_key: Value of the ``X-API-Key`` request header.

    Raises:
        HTTPException: 401 when the key is missing or wrong.
    """
    expected = get_settings().api_key
    if not expected:
        # Development mode: the operator has explicitly not configured a key.
        return

    if not x_api_key or not hmac.compare_digest(x_api_key, expected):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="A valid X-API-Key header is required.",
        )
