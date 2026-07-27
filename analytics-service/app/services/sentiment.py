"""Lexicon-based sentiment analysis tuned for student feedback.

Why a lexicon rather than a fine-tuned transformer: the deployment target is a
512 MB Fargate task that must cold-start in seconds, and the input domain is
narrow and predictable. A curated lexicon with negation and intensifier
handling gets most of the accuracy of a large model on this corpus at roughly
none of the cost, and - unlike a black-box model - every score it produces can
be explained by pointing at the terms that produced it.

The scorer handles three things a naive bag-of-words misses:

* **Negation.** "not helpful" must not count as positive. A negator flips the
  polarity of the next few tokens.
* **Intensifiers and dampeners.** "extremely slow" is worse than "slow";
  "slightly delayed" is milder than "delayed".
* **Domain vocabulary.** Words like *revaluation*, *ragging* and *undercooked*
  carry sentiment in a university context that a general-purpose lexicon
  would score as neutral.
"""

from __future__ import annotations

import math
import re
from dataclasses import dataclass

from app.models.schemas import SentimentLabel

# --------------------------------------------------------------------------
# Lexicon. Weights are in [-2.0, 2.0]; magnitude reflects intensity.
# --------------------------------------------------------------------------

NEGATIVE_TERMS: dict[str, float] = {
    # General dissatisfaction
    "bad": -1.2, "poor": -1.4, "terrible": -1.9, "awful": -1.9, "horrible": -1.9,
    "worst": -2.0, "useless": -1.7, "pathetic": -1.8, "disappointing": -1.5,
    "disappointed": -1.5, "unacceptable": -1.8, "frustrating": -1.5, "frustrated": -1.5,
    "annoying": -1.2, "unhappy": -1.3, "upset": -1.3, "angry": -1.6, "furious": -1.9,
    "complaint": -1.0, "complain": -1.0, "issue": -0.7, "problem": -0.9, "problems": -0.9,
    "concern": -0.7, "concerned": -0.8, "suffering": -1.6, "struggling": -1.2,
    "harassment": -2.0, "ragging": -2.0, "discrimination": -2.0, "unfair": -1.5,
    "negligence": -1.7, "negligent": -1.7, "ignored": -1.4, "ignoring": -1.4,
    # Failure and breakage
    "broken": -1.4, "damaged": -1.3, "faulty": -1.4, "malfunctioning": -1.4,
    "failing": -1.4, "failed": -1.4, "failure": -1.4, "crash": -1.3, "crashes": -1.3,
    "crashing": -1.3, "freezes": -1.2, "freezing": -1.2, "hangs": -1.1, "stuck": -1.1,
    "unusable": -1.8, "unavailable": -1.2, "inaccessible": -1.3, "outage": -1.4,
    "leaking": -1.3, "leak": -1.2, "dirty": -1.4, "filthy": -1.8, "unhygienic": -1.8,
    "undercooked": -1.6, "stale": -1.5, "spoiled": -1.7, "infested": -1.9,
    # Delay and absence
    "delay": -1.1, "delayed": -1.1, "delays": -1.1, "late": -1.0, "slow": -1.1,
    "pending": -0.8, "waiting": -0.8, "postponed": -1.0, "cancelled": -1.1,
    "missing": -1.2, "lost": -1.2, "shortage": -1.3, "insufficient": -1.3,
    "inadequate": -1.4, "lacking": -1.2, "overcrowded": -1.3, "congested": -1.1,
    "never": -1.0, "nobody": -1.1, "repeatedly": -0.9, "again": -0.5,
    # Cost and fairness
    "overcharged": -1.7, "expensive": -1.0, "wrong": -1.2, "incorrect": -1.2,
    "error": -1.2, "mistake": -1.1, "denied": -1.4, "rejected": -1.2,
    "unsafe": -1.9, "dangerous": -1.9, "hazard": -1.8, "injury": -1.9, "risk": -1.2,
}

POSITIVE_TERMS: dict[str, float] = {
    "good": 1.1, "great": 1.5, "excellent": 1.9, "outstanding": 1.9, "exceptional": 1.9,
    "wonderful": 1.7, "fantastic": 1.8, "amazing": 1.8, "brilliant": 1.7, "superb": 1.8,
    "best": 1.7, "perfect": 1.8, "impressive": 1.5, "commendable": 1.6, "remarkable": 1.5,
    "appreciate": 1.5, "appreciated": 1.5, "appreciation": 1.5, "thanks": 1.3,
    "thank": 1.3, "grateful": 1.5, "gratitude": 1.5, "kudos": 1.5, "praise": 1.4,
    "helpful": 1.4, "supportive": 1.4, "responsive": 1.3, "prompt": 1.3, "quick": 1.0,
    "efficient": 1.3, "smooth": 1.2, "seamless": 1.3, "reliable": 1.3, "consistent": 1.0,
    "improved": 1.2, "improvement": 1.1, "better": 1.0, "resolved": 1.2, "fixed": 1.1,
    "satisfied": 1.4, "satisfying": 1.4, "happy": 1.4, "pleased": 1.3, "delighted": 1.7,
    "clean": 1.1, "comfortable": 1.1, "convenient": 1.1, "accessible": 1.0,
    "knowledgeable": 1.4, "engaging": 1.3, "clear": 0.9, "well": 0.8, "welcome": 1.0,
}

# Terms that invert the polarity of what follows them.
NEGATORS: frozenset[str] = frozenset({
    "not", "no", "never", "none", "neither", "nor", "cannot", "cant", "wont",
    "dont", "doesnt", "didnt", "isnt", "arent", "wasnt", "werent", "hasnt",
    "havent", "hadnt", "shouldnt", "wouldnt", "couldnt", "without", "lacks",
    "lack", "hardly", "barely", "rarely",
})

# Multipliers applied to the term that follows.
INTENSIFIERS: dict[str, float] = {
    "very": 1.4, "extremely": 1.8, "highly": 1.4, "really": 1.3, "so": 1.2,
    "totally": 1.5, "completely": 1.5, "absolutely": 1.6, "utterly": 1.7,
    "incredibly": 1.6, "terribly": 1.6, "seriously": 1.4, "deeply": 1.4,
    "constantly": 1.4, "always": 1.3, "consistently": 1.3, "repeatedly": 1.4,
    "extraordinarily": 1.7, "exceptionally": 1.6, "particularly": 1.2,
}

DAMPENERS: dict[str, float] = {
    "slightly": 0.5, "somewhat": 0.6, "a bit": 0.6, "little": 0.6, "marginally": 0.5,
    "occasionally": 0.6, "sometimes": 0.7, "mildly": 0.5, "fairly": 0.8, "quite": 0.9,
}

# How many tokens after a negator stay flipped.
NEGATION_WINDOW = 3

# Boundary tokens that end a negation scope early.
CLAUSE_BREAKS: frozenset[str] = frozenset({"but", "however", "although", "though", "yet"})

_TOKEN_PATTERN = re.compile(r"[a-z']+")

# Score thresholds separating the three labels.
POSITIVE_THRESHOLD = 0.15
NEGATIVE_THRESHOLD = -0.15


@dataclass(frozen=True)
class SentimentResult:
    """Outcome of scoring one document.

    Attributes:
        label: Discrete classification.
        score: Normalised polarity in ``[-1.0, 1.0]``.
        matched_terms: Lexicon hits, for explainability.
    """

    label: SentimentLabel
    score: float
    matched_terms: list[str]


def tokenize(text: str) -> list[str]:
    """Lower-case and split into alphabetic tokens, stripping apostrophes.

    Args:
        text: Raw input.

    Returns:
        A list of normalised tokens.
    """
    return [token.replace("'", "") for token in _TOKEN_PATTERN.findall(text.lower())]


def analyze_sentiment(text: str) -> SentimentResult:
    """Score the polarity of a piece of feedback.

    Args:
        text: The feedback title and body.

    Returns:
        A :class:`SentimentResult`. Empty or purely factual text scores
        ``0.0`` and is labelled ``NEUTRAL``.

    Examples:
        >>> analyze_sentiment("The staff were extremely helpful").label
        <SentimentLabel.POSITIVE: 'POSITIVE'>
        >>> analyze_sentiment("The staff were not helpful at all").label
        <SentimentLabel.NEGATIVE: 'NEGATIVE'>
    """
    tokens = tokenize(text)
    if not tokens:
        return SentimentResult(SentimentLabel.NEUTRAL, 0.0, [])

    total = 0.0
    matched: list[str] = []
    negation_remaining = 0
    multiplier = 1.0

    for token in tokens:
        if token in CLAUSE_BREAKS:
            negation_remaining = 0
            multiplier = 1.0
            continue

        if token in NEGATORS:
            negation_remaining = NEGATION_WINDOW
            continue

        if token in INTENSIFIERS:
            multiplier = INTENSIFIERS[token]
            continue

        if token in DAMPENERS:
            multiplier = DAMPENERS[token]
            continue

        weight = NEGATIVE_TERMS.get(token) or POSITIVE_TERMS.get(token)
        if weight is None:
            if negation_remaining:
                negation_remaining -= 1
            continue

        value = weight * multiplier
        if negation_remaining:
            # "not helpful" -> negative, but damped: absence of praise is
            # weaker evidence than explicit criticism.
            value = -value * 0.8
            negation_remaining -= 1

        total += value
        matched.append(token)
        multiplier = 1.0

    score = _normalise(total, len(matched))
    return SentimentResult(_label_for(score), round(score, 4), matched[:12])


def _normalise(total: float, hits: int) -> float:
    """Squash a raw score into ``[-1, 1]``.

    A hyperbolic tangent over the mean term weight keeps a document with one
    very strong term from outranking a document with six moderately negative
    ones, while still bounding the result.

    Args:
        total: Sum of weighted term scores.
        hits: Number of lexicon terms matched.

    Returns:
        The normalised polarity.
    """
    if hits == 0:
        return 0.0
    mean = total / math.sqrt(hits)
    return math.tanh(mean / 2.0)


def _label_for(score: float) -> SentimentLabel:
    """Map a normalised score onto a discrete label."""
    if score >= POSITIVE_THRESHOLD:
        return SentimentLabel.POSITIVE
    if score <= NEGATIVE_THRESHOLD:
        return SentimentLabel.NEGATIVE
    return SentimentLabel.NEUTRAL
