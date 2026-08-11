
from __future__ import annotations

from dataclasses import dataclass

from app.models.schemas import Category, Priority, SentimentLabel
from app.services.sentiment import tokenize


CRITICAL_TERMS: frozenset[str] = frozenset({
    "unsafe", "dangerous", "danger", "hazard", "hazardous", "injury", "injured",
    "accident", "fire", "electrocution", "shock", "collapse", "harassment",
    "ragging", "abuse", "assault", "threat", "threatened", "discrimination",
    "medical", "emergency", "hospital", "poisoning", "infestation", "infested",
})


HIGH_TERMS: frozenset[str] = frozenset({
    "urgent", "urgently", "immediately", "immediate", "critical", "severe",
    "repeatedly", "constantly", "every", "daily", "weeks", "months",
    "unusable", "blocked", "stranded", "missing", "deadline", "exam",
    "examination", "result", "results", "fee", "fees", "scholarship",
    "refund", "outage", "cancelled", "failure", "failed",
})


BLOCKING_CATEGORIES: frozenset[Category] = frozenset({
    Category.EXAMINATION,
    Category.ADMINISTRATION,
})


STRONG_NEGATIVE = -0.5


@dataclass(frozen=True)
class PriorityDecision:

    priority: Priority
    reason: str


def infer_priority(text: str,
                   sentiment_label: SentimentLabel,
                   sentiment_score: float,
                   category: Category) -> PriorityDecision:
    tokens = set(tokenize(text))

    critical_hits = tokens & CRITICAL_TERMS
    if critical_hits:
        return PriorityDecision(
            Priority.URGENT,
            f"Safety or welfare vocabulary detected: {', '.join(sorted(critical_hits)[:3])}",
        )

    strongly_negative = sentiment_score <= STRONG_NEGATIVE

    if strongly_negative and category in BLOCKING_CATEGORIES:
        return PriorityDecision(
            Priority.URGENT,
            f"Strong dissatisfaction in {category.value}, which blocks academic progress",
        )


    if sentiment_label is SentimentLabel.POSITIVE:
        return PriorityDecision(
            Priority.LOW,
            "Positive feedback; urgency vocabulary present but not a complaint",
        )

    high_hits = tokens & HIGH_TERMS
    if high_hits or strongly_negative:
        reason = (
            f"Urgency indicators present: {', '.join(sorted(high_hits)[:3])}"
            if high_hits
            else f"Strongly negative sentiment ({sentiment_score:.2f})"
        )
        return PriorityDecision(Priority.HIGH, reason)

    if sentiment_label is SentimentLabel.NEGATIVE:
        return PriorityDecision(Priority.MEDIUM, "Negative sentiment without urgency indicators")

    return PriorityDecision(Priority.LOW, "Neutral or positive feedback")
