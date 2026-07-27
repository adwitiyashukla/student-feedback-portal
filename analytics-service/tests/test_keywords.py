"""Tests for keyword extraction."""

from __future__ import annotations

from app.services.keywords import extract_keywords


def test_extracts_salient_terms() -> None:
    keywords = extract_keywords(
        "The hostel mess food is undercooked and the kitchen is unhygienic")
    assert any(term in keywords for term in ("hostel", "kitchen", "undercooked", "unhygienic"))


def test_drops_stop_words() -> None:
    keywords = extract_keywords("The students please request that this is looked at")
    assert "students" not in keywords
    assert "please" not in keywords


def test_drops_short_tokens_and_numbers() -> None:
    keywords = extract_keywords("Room 204 has 3 bad fans in it")
    assert all(len(term) >= 4 and not term.isdigit() for term in keywords)


def test_respects_limit() -> None:
    text = "library hostel transport examination faculty infrastructure academic administration network printer"
    assert len(extract_keywords(text, limit=4)) <= 4


def test_empty_input_returns_empty_list() -> None:
    assert extract_keywords("") == []
