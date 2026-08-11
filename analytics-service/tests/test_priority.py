
from __future__ import annotations

import pytest

from app.models.schemas import Category, Priority, SentimentLabel
from app.services.priority import infer_priority


class TestCriticalEscalation:
    @pytest.mark.parametrize("text", [
        "The lathe machines are unsafe and missing their guards",
        "A student was injured in the workshop yesterday",
        "There is an ongoing ragging problem in the hostel",
        "Exposed wiring is a serious electrical hazard",
    ])
    def test_safety_vocabulary_forces_urgent(self, text: str) -> None:
        decision = infer_priority(text, SentimentLabel.NEGATIVE, -0.6, Category.INFRASTRUCTURE)
        assert decision.priority is Priority.URGENT
        assert decision.reason

    def test_safety_beats_positive_sentiment(self) -> None:

        decision = infer_priority(
            "Thanks for the good work, but the wiring is dangerous",
            SentimentLabel.POSITIVE, 0.4, Category.INFRASTRUCTURE)
        assert decision.priority is Priority.URGENT


class TestBlockingCategories:
    def test_strong_negative_in_examination_is_urgent(self) -> None:
        decision = infer_priority(
            "My marks are wrong and nobody will correct them",
            SentimentLabel.NEGATIVE, -0.75, Category.EXAMINATION)
        assert decision.priority is Priority.URGENT

    def test_strong_negative_elsewhere_is_only_high(self) -> None:
        decision = infer_priority(
            "The common room is poorly maintained",
            SentimentLabel.NEGATIVE, -0.75, Category.OTHER)
        assert decision.priority is Priority.HIGH


class TestPraiseIsNotEscalated:

    def test_praise_mentioning_an_outage_stays_low(self) -> None:
        decision = infer_priority(
            "The network outage was restored very quickly and updates were excellent",
            SentimentLabel.POSITIVE, 0.8, Category.IT_SUPPORT)
        assert decision.priority is Priority.LOW

    def test_praise_about_fast_results_stays_low(self) -> None:
        decision = infer_priority(
            "Exam results were published immediately, thank you",
            SentimentLabel.POSITIVE, 0.7, Category.EXAMINATION)
        assert decision.priority is Priority.LOW

    def test_but_praise_reporting_a_hazard_still_escalates(self) -> None:
        decision = infer_priority(
            "Great teaching this term, though the lab wiring is dangerous",
            SentimentLabel.POSITIVE, 0.5, Category.INFRASTRUCTURE)
        assert decision.priority is Priority.URGENT


class TestGradations:
    def test_urgency_vocabulary_raises_to_high(self) -> None:
        decision = infer_priority(
            "This needs attention immediately",
            SentimentLabel.NEUTRAL, -0.1, Category.OTHER)
        assert decision.priority is Priority.HIGH

    def test_mild_negative_is_medium(self) -> None:
        decision = infer_priority(
            "The noticeboard is a bit disorganised",
            SentimentLabel.NEGATIVE, -0.25, Category.OTHER)
        assert decision.priority is Priority.MEDIUM

    def test_positive_feedback_is_low(self) -> None:
        decision = infer_priority(
            "The fest was organised very well",
            SentimentLabel.POSITIVE, 0.8, Category.OTHER)
        assert decision.priority is Priority.LOW

    def test_neutral_feedback_is_low(self) -> None:
        decision = infer_priority(
            "Asking about the elective list",
            SentimentLabel.NEUTRAL, 0.0, Category.ACADEMIC)
        assert decision.priority is Priority.LOW

    def test_every_decision_carries_a_reason(self) -> None:
        for label, score in [(SentimentLabel.NEGATIVE, -0.9), (SentimentLabel.NEUTRAL, 0.0),
                             (SentimentLabel.POSITIVE, 0.7)]:
            assert infer_priority("some feedback", label, score, Category.OTHER).reason
