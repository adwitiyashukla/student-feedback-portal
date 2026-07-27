"""Priority inference from sentiment, category and urgency vocabulary.

Priority is not learned. There is no labelled priority data to learn from, and
inventing one would produce a model whose mistakes nobody could explain. This
is an explicit, auditable rule set instead — which is also what a university
would need to sign off on before letting software escalate a complaint.
"""

from __future__ import annotations

from dataclasses import dataclass

from app.models.schemas import Category, Priority, SentimentLabel
from app.services.sentiment import tokenize

# Vocabulary implying a safety or welfare risk. Any hit forces URGENT.
CRITICAL_TERMS: frozenset[str] = frozenset({
    "unsafe", "dangerous", "danger", "hazard", "hazardous", "injury", "injured",
    "accident", "fire", "electrocution", "shock", "collapse", "harassment",
    "ragging", "abuse", "assault", "threat", "threatened", "discrimination",
    "medical", "emergency", "hospital", "poisoning", "infestation", "infested",
})

# Vocabulary implying material disruption or repeated failure.
HIGH_TERMS: frozenset[str] = frozenset({
    "urgent", "urgently", "immediately", "immediate", "critical", "severe",
    "repeatedly", "constantly", "every", "daily", "weeks", "months",
    "unusable", "blocked", "stranded", "missing", "deadline", "exam",
    "examination", "result", "results", "fee", "fees", "scholarship",
    "refund", "outage", "cancelled", "failure", "failed",
})

# Categories where a problem blocks a student's academic progress.
BLOCKING_CATEGORIES: frozenset[Category] = frozenset({
    Category.EXAMINATION,
    Category.ADMINISTRATION,
})

# Sentiment below this counts as strong dissatisfaction.
STRONG_NEGATIVE = -0.5


@dataclass(frozen=True)
class PriorityDecision:
    """Assigned priority with the reason for it.

    Attributes:
        priority: The resulting level.
        reason: Human-readable justification, surfaced in the admin UI.
    """

    priority: Priority
    reason: str


def infer_priority(text: str,
                   sentiment_label: SentimentLabel,
                   sentiment_score: float,
                   category: Category) -> PriorityDecision:
    """Decide how urgently a piece of feedback should be handled.

    The rules are checked in descending severity and the first match wins:

    1. safety or welfare vocabulary -> ``URGENT``;
    2. strong dissatisfaction in a progress-blocking category -> ``URGENT``;
    3. urgency vocabulary, or strong dissatisfaction anywhere -> ``HIGH``;
    4. any negative sentiment -> ``MEDIUM``;
    5. positive or neutral feedback -> ``LOW``.

    Args:
        text: Title and body concatenated.
        sentiment_label: Discrete sentiment.
        sentiment_score: Normalised polarity in ``[-1, 1]``.
        category: Predicted subject area.

    Returns:
        A :class:`PriorityDecision`.

    Examples:
        >>> infer_priority("unsafe machinery", SentimentLabel.NEGATIVE, -0.8,
        ...                Category.INFRASTRUCTURE).priority
        <Priority.URGENT: 'URGENT'>
    """
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

    # Praise is not a complaint. Urgency vocabulary routinely appears in
    # positive messages - "the outage was fixed immediately", "results were
    # released quickly" - and escalating those buries the real queue. Only a
    # safety term, handled above, can override this.
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
