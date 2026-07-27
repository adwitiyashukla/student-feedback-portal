"""FastAPI application entry point.

Serves the feedback analytics API consumed by the Spring Boot backend. The
model is loaded once during the lifespan startup hook rather than per request;
a cold container trains it in-process, which takes a few seconds and then never
happens again for that image.
"""

from __future__ import annotations

import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.requests import Request
from fastapi.responses import JSONResponse

from app.api.routes import router
from app.core.config import get_settings
from app.services.classifier import classifier

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-8s %(name)s - %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    """Load or train the model on startup."""
    settings = get_settings()
    if not settings.api_key:
        logger.warning(
            "ANALYTICS_API_KEY is not set - the API is unauthenticated. "
            "Acceptable for local development only."
        )
    classifier.load_or_train()
    logger.info("%s %s ready", settings.app_name, settings.version)
    yield
    logger.info("Shutting down")


def create_app() -> FastAPI:
    """Build the application. Factored out so tests can construct it directly."""
    settings = get_settings()

    app = FastAPI(
        title="Feedback Analytics Service",
        description=(
            "Sentiment analysis, category classification and priority inference for "
            "the Student Feedback Portal.\n\n"
            "Called server-to-server by the Spring Boot backend and authenticated "
            "with a shared `X-API-Key` header. It is never exposed to browsers."
        ),
        version=settings.version,
        lifespan=lifespan,
        docs_url="/docs",
        redoc_url="/redoc",
    )

    # The only client is the backend, inside the compose network or the VPC.
    app.add_middleware(
        CORSMiddleware,
        allow_origins=[],
        allow_credentials=False,
        allow_methods=["POST", "GET"],
        allow_headers=["X-API-Key", "Content-Type"],
    )

    app.include_router(router)

    @app.exception_handler(Exception)
    async def unhandled_exception_handler(request: Request, exc: Exception) -> JSONResponse:
        """Log the detail, return none of it."""
        logger.exception("Unhandled error on %s %s", request.method, request.url.path)
        return JSONResponse(
            status_code=500,
            content={"detail": "Internal server error"},
        )

    return app


app = create_app()
