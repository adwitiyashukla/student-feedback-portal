"""Request and response models.

These are the contract with the Spring Boot backend. Field names are snake_case
because that is what the Java ``AnalysisResult`` record maps with
``@JsonProperty``.
"""

from __future__ import annotations

from enum import Enum

from pydantic import BaseModel, Field


class SentimentLabel(str, Enum):
    POSITIVE = "POSITIVE"
    NEUTRAL = "NEUTRAL"
    NEGATIVE = "NEGATIVE"


class Priority(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    URGENT = "URGENT"


class Category(str, Enum):
    ACADEMIC = "ACADEMIC"
    FACULTY = "FACULTY"
    EXAMINATION = "EXAMINATION"
    INFRASTRUCTURE = "INFRASTRUCTURE"
    HOSTEL = "HOSTEL"
    LIBRARY = "LIBRARY"
    TRANSPORT = "TRANSPORT"
    ADMINISTRATION = "ADMINISTRATION"
    IT_SUPPORT = "IT_SUPPORT"
    OTHER = "OTHER"


class AnalyzeRequest(BaseModel):
    """One piece of feedback to classify."""

    title: str = Field(default="", max_length=300, description="Feedback title")
    text: str = Field(..., min_length=1, max_length=20_000, description="Feedback body")

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "title": "Mess food quality has dropped sharply",
                    "text": (
                        "The dinner served in the hostel mess has been repetitive and often "
                        "undercooked since the vendor changed. Several students reported "
                        "stomach problems."
                    ),
                }
            ]
        }
    }

    @property
    def combined(self) -> str:
        """Title and body joined, which is what the models are trained on."""
        return f"{self.title} {self.text}".strip()


class AnalyzeResponse(BaseModel):
    """Model output returned to the backend."""

    sentiment_label: SentimentLabel
    sentiment_score: float = Field(..., ge=-1.0, le=1.0, description="Signed polarity")
    category: Category
    category_confidence: float = Field(..., ge=0.0, le=1.0)
    priority: Priority
    keywords: list[str] = Field(default_factory=list, max_length=10)
    model_version: str


class BatchAnalyzeRequest(BaseModel):
    items: list[AnalyzeRequest] = Field(..., min_length=1, max_length=200)


class BatchAnalyzeResponse(BaseModel):
    results: list[AnalyzeResponse]
    processed: int


class HealthResponse(BaseModel):
    status: str
    service: str
    version: str
    model_loaded: bool
    model_version: str


class ModelInfoResponse(BaseModel):
    model_version: str
    categories: list[str]
    training_examples: int
    vocabulary_size: int
    cross_val_accuracy: float | None = None
