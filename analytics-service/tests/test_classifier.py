"""Tests for the TF-IDF category classifier."""

from __future__ import annotations

import pytest

from app.models.schemas import Category
from app.services.classifier import classifier
from app.services.training_data import build_corpus, evaluation_set

# Below this the model is not fit for triage and the build should fail.
MIN_HOLDOUT_ACCURACY = 0.80


class TestCorpus:
    def test_corpus_is_balanced(self) -> None:
        _, labels = build_corpus()
        counts = {label: labels.count(label) for label in set(labels)}
        assert len(set(counts.values())) == 1, f"unbalanced corpus: {counts}"

    def test_corpus_is_deterministic(self) -> None:
        assert build_corpus() == build_corpus()

    def test_evaluation_set_is_disjoint_from_training(self) -> None:
        train_texts, _ = build_corpus()
        eval_texts, _ = evaluation_set()
        assert not (set(train_texts) & set(eval_texts))

    def test_every_category_is_evaluated(self) -> None:
        _, train_labels = build_corpus()
        _, eval_labels = evaluation_set()
        assert set(eval_labels) == set(train_labels)


class TestPrediction:
    def test_model_is_loaded(self) -> None:
        assert classifier.is_loaded
        assert classifier.vocabulary_size > 0

    @pytest.mark.parametrize(("text", "expected"), [
        ("The mess food is undercooked and the hostel water supply is cut off", Category.HOSTEL),
        ("Revaluation results for the semester exam have not been published", Category.EXAMINATION),
        ("Campus wifi keeps disconnecting and the lab computers are very slow", Category.IT_SUPPORT),
        ("The route 7 college bus arrives late every morning", Category.TRANSPORT),
        ("The reading room in the central library closes too early", Category.LIBRARY),
    ])
    def test_predicts_obvious_categories(self, text: str, expected: Category) -> None:
        assert classifier.predict(text).category is expected

    def test_confidence_is_a_probability(self) -> None:
        prediction = classifier.predict("The hostel mess food is terrible")
        assert 0.0 <= prediction.confidence <= 1.0

    def test_runner_up_is_populated(self) -> None:
        assert classifier.predict("The library is closed").runner_up is not None

    def test_empty_input_falls_back_to_other(self) -> None:
        prediction = classifier.predict("   ")
        assert prediction.category is Category.OTHER
        assert prediction.confidence == 0.0


class TestAccuracy:
    def test_holdout_accuracy_meets_threshold(self) -> None:
        """The gate that matters: hand-written tickets the model never saw."""
        texts, labels = evaluation_set()
        correct = sum(
            classifier.predict(text).category.value == label
            for text, label in zip(texts, labels, strict=False)
        )
        accuracy = correct / len(labels)
        assert accuracy >= MIN_HOLDOUT_ACCURACY, (
            f"held-out accuracy {accuracy:.3f} below threshold {MIN_HOLDOUT_ACCURACY}"
        )
