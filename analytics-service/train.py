from __future__ import annotations

import logging
import sys

from app.core.config import get_settings
from app.services.classifier import classifier

logging.basicConfig(level=logging.INFO, format="%(levelname)-8s %(message)s")


ACCEPTANCE_THRESHOLD = 0.80


def main() -> int:
    settings = get_settings()
    holdout = classifier.train(settings.model_path)

    print()
    print("=" * 66)
    print(f"  Model written to           : {settings.model_path}")
    print(f"  Training examples          : {classifier.training_examples}")
    print(f"  Categories                 : {len(classifier.categories)}")
    print(f"  Vocabulary size            : {classifier.vocabulary_size}")
    print(f"  5-fold CV (synthetic)      : {classifier.cv_accuracy:.4f}")
    print(f"  Held-out (hand-written)    : {holdout:.4f}   <-- the real number")
    print("=" * 66)
    print("  CV accuracy is high because the training corpus is generated from")
    print("  templates and the folds share vocabulary by construction. Only the")
    print("  held-out score is quoted in the README.")
    print()

    if holdout < ACCEPTANCE_THRESHOLD:
        print(
            f"ERROR: held-out accuracy {holdout:.4f} is below the "
            f"{ACCEPTANCE_THRESHOLD:.2f} acceptance threshold",
            file=sys.stderr,
        )
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
