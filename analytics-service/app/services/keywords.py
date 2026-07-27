"""Keyword extraction for the admin triage view.

Uses the fitted TF-IDF vocabulary where available so the terms surfaced are the
same ones the classifier actually weighted, falling back to a frequency count
when the model has not loaded.
"""

from __future__ import annotations

import re
from collections import Counter

from app.services.classifier import classifier
from app.services.sentiment import tokenize

STOP_WORDS: frozenset[str] = frozenset({
    "the", "a", "an", "and", "or", "but", "is", "are", "was", "were", "be",
    "been", "being", "to", "of", "in", "on", "at", "for", "with", "from",
    "by", "as", "it", "its", "this", "that", "these", "those", "there",
    "here", "we", "our", "us", "i", "my", "me", "you", "your", "they",
    "them", "their", "he", "she", "his", "her", "has", "have", "had",
    "do", "does", "did", "will", "would", "should", "could", "can", "may",
    "not", "no", "so", "if", "than", "then", "when", "which", "who", "what",
    "student", "students", "college", "university", "campus", "please",
    "kindly", "request", "requesting", "sir", "madam", "very", "also",
    "since", "still", "even", "only", "more", "most", "some", "any",
})

MIN_TOKEN_LENGTH = 4
_NUMERIC = re.compile(r"^\d+$")


def extract_keywords(text: str, limit: int = 8) -> list[str]:
    """Pick the most informative terms in a document.

    Args:
        text: Title and body concatenated.
        limit: Maximum number of keywords to return.

    Returns:
        Up to ``limit`` terms, most significant first.
    """
    tokens = [
        token for token in tokenize(text)
        if len(token) >= MIN_TOKEN_LENGTH
        and token not in STOP_WORDS
        and not _NUMERIC.match(token)
    ]
    if not tokens:
        return []

    weights = _tfidf_weights()
    if weights:
        scored = Counter()
        for token in tokens:
            # Unknown terms still score, just below any known term.
            scored[token] += weights.get(token, 0.5)
        return [term for term, _ in scored.most_common(limit)]

    return [term for term, _ in Counter(tokens).most_common(limit)]


def _tfidf_weights() -> dict[str, float]:
    """Inverse-document-frequency weights from the fitted word vectoriser.

    Returns:
        A term-to-IDF mapping, or an empty dict when the model is not loaded.
    """
    if not classifier.is_loaded:
        return {}
    try:
        union = classifier._pipeline.named_steps["features"]
        word_vectorizer = dict(union.transformer_list)["word"]
        vocabulary = word_vectorizer.vocabulary_
        idf = word_vectorizer.idf_
        return {term: float(idf[index]) for term, index in vocabulary.items() if " " not in term}
    except (AttributeError, KeyError):
        return {}
