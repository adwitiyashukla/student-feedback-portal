"""TF-IDF + logistic-regression classifier for feedback categories.

The pipeline is a word-level and character-level TF-IDF union feeding a
multinomial logistic regression. Character n-grams are included because student
feedback contains a lot of typos and inconsistent spacing, and character
features degrade gracefully where word features miss entirely.

The fitted pipeline is persisted with ``joblib``. On startup the service loads
it if present and trains it in-process otherwise, so a fresh container is
usable without a separate training step - the first boot simply takes a few
seconds longer.
"""

from __future__ import annotations

import logging
import threading
from dataclasses import dataclass
from pathlib import Path

import joblib
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import cross_val_score
from sklearn.pipeline import FeatureUnion, Pipeline

from app.core.config import get_settings
from app.models.schemas import Category
from app.services.training_data import build_corpus, category_names, evaluation_set

logger = logging.getLogger(__name__)

MODEL_VERSION = "tfidf-logreg-v2.0.0"

# Words that carry no signal in this domain: they appear in every category.
DOMAIN_STOP_WORDS = [
    "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
    "to", "of", "in", "on", "at", "for", "with", "and", "or", "but",
    "this", "that", "these", "those", "it", "its", "as", "by", "from",
    "student", "students", "college", "university", "campus", "please",
    "would", "like", "request", "requesting", "kindly", "sir", "madam",
]


@dataclass(frozen=True)
class CategoryPrediction:
    """A single category prediction.

    Attributes:
        category: The predicted label.
        confidence: Probability assigned to that label, in ``[0, 1]``.
        runner_up: Second most likely label, useful for triage UIs.
    """

    category: Category
    confidence: float
    runner_up: Category | None = None


def build_pipeline() -> Pipeline:
    """Construct the (unfitted) classification pipeline.

    Returns:
        A scikit-learn :class:`~sklearn.pipeline.Pipeline` combining word and
        character TF-IDF features with a logistic-regression head.
    """
    word_features = TfidfVectorizer(
        analyzer="word",
        ngram_range=(1, 2),
        min_df=2,
        sublinear_tf=True,
        stop_words=DOMAIN_STOP_WORDS,
        strip_accents="unicode",
        lowercase=True,
    )
    char_features = TfidfVectorizer(
        analyzer="char_wb",
        ngram_range=(3, 5),
        min_df=3,
        sublinear_tf=True,
        strip_accents="unicode",
        lowercase=True,
    )

    return Pipeline([
        ("features", FeatureUnion([("word", word_features), ("char", char_features)])),
        ("classifier", LogisticRegression(
            C=4.0,
            max_iter=2000,
            class_weight="balanced",
            solver="lbfgs",
            n_jobs=None,
            random_state=42,
        )),
    ])


class CategoryClassifier:
    """Thread-safe wrapper around the fitted pipeline.

    FastAPI serves requests concurrently, so loading and training are guarded
    by a lock. Prediction itself is read-only on the fitted estimator and needs
    no synchronisation.
    """

    def __init__(self) -> None:
        self._pipeline: Pipeline | None = None
        self._lock = threading.Lock()
        self._training_examples = 0
        self._cv_accuracy: float | None = None
        self._holdout_accuracy: float | None = None

    # -- lifecycle ---------------------------------------------------------

    def load_or_train(self, model_path: Path | None = None) -> None:
        """Load the persisted model, training and saving one if absent.

        Args:
            model_path: Override for the artifact location. Defaults to the
                configured ``ANALYTICS_MODEL_DIR``.
        """
        path = model_path or get_settings().model_path
        with self._lock:
            if path.exists():
                try:
                    payload = joblib.load(path)
                    self._pipeline = payload["pipeline"]
                    self._training_examples = payload.get("training_examples", 0)
                    self._cv_accuracy = payload.get("cv_accuracy")
                    self._holdout_accuracy = payload.get("holdout_accuracy")
                    logger.info("Loaded category model from %s", path)
                    return
                except (KeyError, EOFError, ValueError) as exc:
                    logger.warning("Model at %s is unreadable (%s); retraining", path, exc)

            logger.info("No usable model at %s; training a new one", path)
            self._train_unlocked(path)

    def train(self, model_path: Path | None = None) -> float:
        """Fit the pipeline on the corpus and persist it.

        Args:
            model_path: Where to write the artifact.

        Returns:
            Mean 5-fold cross-validated accuracy.
        """
        path = model_path or get_settings().model_path
        with self._lock:
            return self._train_unlocked(path)

    def _train_unlocked(self, path: Path) -> float:
        texts, labels = build_corpus()
        pipeline = build_pipeline()

        scores = cross_val_score(pipeline, texts, labels, cv=5, scoring="accuracy")
        cv_accuracy = float(np.mean(scores))

        pipeline.fit(texts, labels)

        # The corpus is template-generated, so cross-validated accuracy on it is
        # close to meaningless - the folds share vocabulary by construction. The
        # honest number is accuracy on hand-written tickets the model has never
        # seen, which is what gets reported and gated on.
        holdout_texts, holdout_labels = evaluation_set()
        predictions = pipeline.predict(holdout_texts)
        holdout_accuracy = float(np.mean([
            predicted == expected
            for predicted, expected in zip(predictions, holdout_labels, strict=False)
        ]))

        self._pipeline = pipeline
        self._training_examples = len(texts)
        self._cv_accuracy = cv_accuracy
        self._holdout_accuracy = holdout_accuracy

        path.parent.mkdir(parents=True, exist_ok=True)
        joblib.dump(
            {
                "pipeline": pipeline,
                "training_examples": len(texts),
                "cv_accuracy": cv_accuracy,
                "holdout_accuracy": holdout_accuracy,
                "version": MODEL_VERSION,
            },
            path,
            compress=3,
        )
        logger.info(
            "Trained on %d examples - CV %.3f, held-out %.3f - saved to %s",
            len(texts), cv_accuracy, holdout_accuracy, path,
        )
        return holdout_accuracy

    # -- inference ---------------------------------------------------------

    def predict(self, text: str) -> CategoryPrediction:
        """Classify a piece of feedback.

        Args:
            text: Title and body concatenated.

        Returns:
            The best label with its probability. Falls back to
            :attr:`Category.OTHER` when the model is unavailable or its
            confidence is below the configured floor.
        """
        if self._pipeline is None:
            logger.warning("predict() called before the model was loaded")
            return CategoryPrediction(Category.OTHER, 0.0)

        cleaned = (text or "").strip()
        if not cleaned:
            return CategoryPrediction(Category.OTHER, 0.0)

        probabilities = self._pipeline.predict_proba([cleaned])[0]
        classes = self._pipeline.classes_

        order = np.argsort(probabilities)[::-1]
        best_index = int(order[0])
        confidence = float(probabilities[best_index])
        label = str(classes[best_index])

        runner_up = None
        if len(order) > 1:
            runner_up = _to_category(str(classes[int(order[1])]))

        if confidence < get_settings().min_category_confidence:
            # The model has no real opinion; say so rather than guessing.
            return CategoryPrediction(Category.OTHER, round(confidence, 4), runner_up)

        return CategoryPrediction(_to_category(label), round(confidence, 4), runner_up)

    # -- introspection -----------------------------------------------------

    @property
    def is_loaded(self) -> bool:
        return self._pipeline is not None

    @property
    def training_examples(self) -> int:
        return self._training_examples

    @property
    def cv_accuracy(self) -> float | None:
        return self._cv_accuracy

    @property
    def holdout_accuracy(self) -> float | None:
        """Accuracy on the hand-written evaluation set. The meaningful metric."""
        return self._holdout_accuracy

    @property
    def vocabulary_size(self) -> int:
        """Total feature count across both vectorisers."""
        if self._pipeline is None:
            return 0
        union: FeatureUnion = self._pipeline.named_steps["features"]
        return sum(len(vectorizer.vocabulary_) for _, vectorizer in union.transformer_list)

    @property
    def categories(self) -> list[str]:
        if self._pipeline is None:
            return category_names()
        return [str(label) for label in self._pipeline.classes_]


def _to_category(label: str) -> Category:
    """Map a model label onto the enum, defaulting to ``OTHER``."""
    try:
        return Category(label)
    except ValueError:
        return Category.OTHER


# Module-level singleton; the FastAPI lifespan hook loads it once at startup.
classifier = CategoryClassifier()
