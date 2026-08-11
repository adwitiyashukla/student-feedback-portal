
from __future__ import annotations

import logging

from fastapi import APIRouter, Depends, HTTPException, status

from app.core.config import get_settings
from app.core.security import require_api_key
from app.models.schemas import (
    AnalyzeRequest,
    AnalyzeResponse,
    BatchAnalyzeRequest,
    BatchAnalyzeResponse,
    HealthResponse,
    ModelInfoResponse,
)
from app.services.classifier import MODEL_VERSION, classifier
from app.services.keywords import extract_keywords
from app.services.priority import infer_priority
from app.services.sentiment import analyze_sentiment

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1")


@router.get("/health", response_model=HealthResponse, tags=["Health"])
async def health() -> HealthResponse:
    settings = get_settings()
    return HealthResponse(
        status="UP",
        service=settings.app_name,
        version=settings.version,
        model_loaded=classifier.is_loaded,
        model_version=MODEL_VERSION,
    )


@router.get(
    "/model/info",
    response_model=ModelInfoResponse,
    tags=["Model"],
    dependencies=[Depends(require_api_key)],
)
async def model_info() -> ModelInfoResponse:
    if not classifier.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="The classification model is not loaded yet.",
        )
    return ModelInfoResponse(
        model_version=MODEL_VERSION,
        categories=classifier.categories,
        training_examples=classifier.training_examples,
        vocabulary_size=classifier.vocabulary_size,
        cross_val_accuracy=classifier.cv_accuracy,
    )


@router.post(
    "/analyze",
    response_model=AnalyzeResponse,
    tags=["Analysis"],
    dependencies=[Depends(require_api_key)],
    summary="Classify one piece of feedback",
)
async def analyze(request: AnalyzeRequest) -> AnalyzeResponse:
    return _analyze_one(request)


@router.post(
    "/analyze/batch",
    response_model=BatchAnalyzeResponse,
    tags=["Analysis"],
    dependencies=[Depends(require_api_key)],
    summary="Classify up to 200 items in one call",
)
async def analyze_batch(request: BatchAnalyzeRequest) -> BatchAnalyzeResponse:
    results = [_analyze_one(item) for item in request.items]
    return BatchAnalyzeResponse(results=results, processed=len(results))


@router.post(
    "/model/retrain",
    tags=["Model"],
    dependencies=[Depends(require_api_key)],
    summary="Refit the classifier and persist it",
)
async def retrain() -> dict[str, object]:
    accuracy = classifier.train()
    return {
        "status": "retrained",
        "model_version": MODEL_VERSION,
        "training_examples": classifier.training_examples,
        "cross_val_accuracy": round(accuracy, 4),
    }


def _analyze_one(request: AnalyzeRequest) -> AnalyzeResponse:
    text = request.combined[: get_settings().max_text_length]

    sentiment = analyze_sentiment(text)
    category = classifier.predict(text)
    priority = infer_priority(text, sentiment.label, sentiment.score, category.category)

    return AnalyzeResponse(
        sentiment_label=sentiment.label,
        sentiment_score=sentiment.score,
        category=category.category,
        category_confidence=category.confidence,
        priority=priority.priority,
        keywords=extract_keywords(text),
        model_version=MODEL_VERSION,
    )
