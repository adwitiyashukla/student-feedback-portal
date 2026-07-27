"""Tests for the lexicon-based sentiment scorer."""

from __future__ import annotations

import pytest

from app.models.schemas import SentimentLabel
from app.services.sentiment import analyze_sentiment, tokenize


class TestTokenize:
    def test_lowercases_and_splits(self) -> None:
        assert tokenize("The Mess FOOD is Bad") == ["the", "mess", "food", "is", "bad"]

    def test_strips_punctuation_and_digits(self) -> None:
        assert tokenize("Room 204 -- broken!") == ["room", "broken"]

    def test_handles_contractions(self) -> None:
        assert "doesnt" in tokenize("it doesn't work")

    def test_empty_input(self) -> None:
        assert tokenize("") == []


class TestPolarity:
    @pytest.mark.parametrize("text", [
        "The staff were extremely helpful and responsive",
        "Excellent teaching and outstanding lecture notes",
        "I really appreciate how quickly this was resolved",
        "The new library books are fantastic",
    ])
    def test_positive_documents(self, text: str) -> None:
        result = analyze_sentiment(text)
        assert result.label is SentimentLabel.POSITIVE
        assert result.score > 0

    @pytest.mark.parametrize("text", [
        "The mess food is terrible and often undercooked",
        "Lab computers are unusable, they crash constantly",
        "This is completely unacceptable and nobody responds",
        "The washroom is filthy and unhygienic",
    ])
    def test_negative_documents(self, text: str) -> None:
        result = analyze_sentiment(text)
        assert result.label is SentimentLabel.NEGATIVE
        assert result.score < 0

    @pytest.mark.parametrize("text", [
        "The class is scheduled at 9 am in room 204",
        "Requesting information about the elective allocation process",
        "",
    ])
    def test_neutral_documents(self, text: str) -> None:
        assert analyze_sentiment(text).label is SentimentLabel.NEUTRAL

    def test_score_is_bounded(self) -> None:
        extreme = " ".join(["terrible awful horrible worst pathetic"] * 40)
        assert -1.0 <= analyze_sentiment(extreme).score <= 1.0


class TestNegation:
    def test_negation_flips_positive_to_negative(self) -> None:
        positive = analyze_sentiment("The warden was helpful")
        negated = analyze_sentiment("The warden was not helpful")

        assert positive.label is SentimentLabel.POSITIVE
        assert negated.score < positive.score
        assert negated.label is SentimentLabel.NEGATIVE

    def test_negation_flips_negative_to_positive(self) -> None:
        result = analyze_sentiment("The process was not bad at all")
        assert result.score > 0

    def test_negation_scope_ends_at_clause_break(self) -> None:
        # "not ideal, but the staff were excellent" - the praise must survive.
        result = analyze_sentiment("Not ideal but the staff were excellent and helpful")
        assert result.label is SentimentLabel.POSITIVE

    def test_negation_window_expires(self) -> None:
        # "helpful" sits well beyond the three-token window after "not".
        result = analyze_sentiment("not sure who to contact about this so I asked and they were helpful")
        assert result.score > 0


class TestModifiers:
    def test_intensifier_increases_magnitude(self) -> None:
        plain = analyze_sentiment("The service is slow")
        intense = analyze_sentiment("The service is extremely slow")
        assert abs(intense.score) > abs(plain.score)

    def test_dampener_reduces_magnitude(self) -> None:
        plain = analyze_sentiment("The bus is delayed")
        damped = analyze_sentiment("The bus is slightly delayed")
        assert abs(damped.score) < abs(plain.score)


class TestExplainability:
    def test_matched_terms_are_reported(self) -> None:
        result = analyze_sentiment("The food is terrible and the room is dirty")
        assert "terrible" in result.matched_terms
        assert "dirty" in result.matched_terms

    def test_matched_terms_are_capped(self) -> None:
        result = analyze_sentiment(" ".join(["bad poor terrible awful"] * 20))
        assert len(result.matched_terms) <= 12
