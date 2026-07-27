"""Labelled training corpus for the category classifier.

The corpus is generated from per-category templates rather than checked in as
a flat CSV. Two reasons:

* it stays reviewable — a reader can see the vocabulary each label is built
  from, instead of scrolling a thousand rows;
* it is reproducible — generation is seeded, so ``train.py`` produces the same
  model on every machine and the accuracy figure in the README means something.

In a production deployment this module would be replaced by real historical
tickets exported from the portal. The interface — :func:`build_corpus`
returning ``(texts, labels)`` — is deliberately the same either way, so
swapping in real data changes nothing downstream.
"""

from __future__ import annotations

import random

SEED = 20260725

# Per category: subjects, complaints/observations, and locations or qualifiers.
# The Cartesian product of these, sampled, is the corpus.
TEMPLATES: dict[str, dict[str, list[str]]] = {
    "ACADEMIC": {
        "subject": [
            "the syllabus", "the course curriculum", "the lecture schedule",
            "the assignment deadline", "the elective allocation", "the project guidelines",
            "the tutorial sessions", "the practical lab schedule", "the credit structure",
            "the attendance requirement",
        ],
        "predicate": [
            "is too demanding for one semester", "has not been shared with students",
            "overlaps with other classes", "needs to be revised",
            "was changed without any notice", "does not match the exam pattern",
            "leaves no time for revision", "should include more practical work",
            "is unclear and needs explanation", "conflicts with the placement drive",
        ],
        "context": [
            "for the sixth semester", "in the computer science department",
            "for final year students", "this academic year", "for the current batch",
        ],
    },
    "FACULTY": {
        "subject": [
            "the professor", "the lecturer", "the visiting faculty", "the lab instructor",
            "the course coordinator", "the guest lecturer", "the teaching assistant",
        ],
        "predicate": [
            "explains concepts very clearly", "does not respond to student doubts",
            "rushes through the material", "conducts excellent doubt-clearing sessions",
            "has been absent for several classes", "gives genuinely useful feedback on assignments",
            "teaches at a pace nobody can follow", "prepares outstanding lecture notes",
            "is unavailable during office hours", "makes a difficult subject approachable",
        ],
        "context": [
            "for the signals and systems course", "in the data structures class",
            "during the thermodynamics lectures", "in the database systems module",
            "for the machine learning elective",
        ],
    },
    "EXAMINATION": {
        "subject": [
            "the revaluation result", "the internal assessment marks", "the exam timetable",
            "the answer script", "the hall ticket", "the grade card", "the supplementary exam",
            "the question paper", "the marks entry",
        ],
        "predicate": [
            "has still not been published", "shows an incorrect total",
            "was released only two days before the exam", "is missing from the portal",
            "contained questions outside the syllabus", "has not been updated after revaluation",
            "clashes with another paper", "was declared without any explanation",
            "does not reflect the marks awarded",
        ],
        "context": [
            "for semester five", "for the end-semester examination", "on the student portal",
            "for the data structures paper", "after six weeks of waiting",
        ],
    },
    "INFRASTRUCTURE": {
        "subject": [
            "the classroom", "the seminar hall", "the workshop machinery", "the projector",
            "the ceiling fans", "the drinking water cooler", "the washroom", "the laboratory equipment",
            "the seating arrangement", "the air conditioning",
        ],
        "predicate": [
            "has been out of order for weeks", "is in very poor condition",
            "needs urgent repair", "is missing basic safety guards",
            "has broken furniture that nobody replaces", "is unusable during the afternoon",
            "was recently renovated and is excellent now", "leaks whenever it rains",
            "has insufficient capacity for the batch size",
        ],
        "context": [
            "in the main academic block", "in room 204", "on the third floor",
            "in the mechanical workshop", "near the department office",
        ],
    },
    "HOSTEL": {
        "subject": [
            "the mess food", "the hostel water supply", "the room allocation", "the warden",
            "the laundry service", "the hostel Wi-Fi", "the common room", "the night canteen",
            "the cleaning staff", "the hostel gate timing",
        ],
        "predicate": [
            "has become very poor since the vendor changed", "is cut off every afternoon",
            "is repetitive and often undercooked", "has improved considerably this term",
            "is not maintained properly at all", "causes problems for students with late labs",
            "needs immediate attention from the administration", "is unhygienic and unsafe",
            "responds quickly to student requests",
        ],
        "context": [
            "in block B", "in the boys hostel", "in the girls hostel",
            "for first year residents", "throughout this month",
        ],
    },
    "LIBRARY": {
        "subject": [
            "the reading room", "the book issue counter", "the digital library subscription",
            "the reference section", "the journal access", "the library timings",
            "the new book acquisitions", "the study cubicles", "the photocopy service",
        ],
        "predicate": [
            "closes far too early during exams", "is not accessible from outside campus",
            "has an excellent new collection this year", "does not have enough copies of the textbook",
            "should be extended to eleven at night", "is very well maintained",
            "has an outdated catalogue system", "requires a longer borrowing period",
            "needs more seating during the exam period",
        ],
        "context": [
            "in the central library", "for final year project work", "during the examination weeks",
            "for the algorithms textbook", "for postgraduate students",
        ],
    },
    "TRANSPORT": {
        "subject": [
            "the college bus", "the route 7 service", "the bus pass", "the shuttle timing",
            "the bus driver", "the pickup point", "the transport fee", "the evening bus",
        ],
        "predicate": [
            "arrives twenty five minutes late every day", "does not stop at the notified point",
            "is always overcrowded in the morning", "has been very punctual this semester",
            "was cancelled without any prior notice", "needs an additional trip in the evening",
            "charges more than the notified amount", "makes students miss the first period",
        ],
        "context": [
            "on the morning route", "from the Kondapur stop", "for day scholars",
            "during the last two weeks", "on weekdays",
        ],
    },
    "ADMINISTRATION": {
        "subject": [
            "the fee receipt", "the scholarship reimbursement", "the bonafide certificate",
            "the transcript request", "the admission office", "the accounts counter",
            "the document verification", "the refund process", "the academic calendar",
        ],
        "predicate": [
            "has been pending since March", "shows an amount higher than the fee structure",
            "takes several visits to complete", "was processed very quickly and smoothly",
            "requires documents that were never mentioned", "is released far too late to be useful",
            "has not been credited despite approval", "needs a clearer online process",
        ],
        "context": [
            "for the current academic year", "at the accounts section", "for merit scholarship holders",
            "in the administrative block", "for the hostel fee",
        ],
    },
    "IT_SUPPORT": {
        "subject": [
            "the campus Wi-Fi", "the lab computers", "the student portal login",
            "the online quiz platform", "the network connection", "the printer in the lab",
            "the email account", "the software licence", "the learning management system",
        ],
        "predicate": [
            "keeps disconnecting during online tests", "takes ten minutes to boot",
            "has no signal on the upper floors", "was restored very quickly after the outage",
            "freezes constantly during practical sessions", "rejects the correct password",
            "is not installed on any of the machines", "needs a serious hardware upgrade",
            "has been down for three days now",
        ],
        "context": [
            "in the block C programming lab", "in the new academic block",
            "for the entire batch", "during practical hours", "since last Monday",
        ],
    },
    "OTHER": {
        "subject": [
            "the sports ground", "the cultural festival", "the student council",
            "the placement cell", "the health centre", "the campus canteen",
            "the parking area", "the notice board", "the alumni event",
        ],
        "predicate": [
            "could be organised much better", "was handled very professionally",
            "needs more student participation", "has limited facilities at the moment",
            "clashes with academic commitments", "should be announced further in advance",
            "does not have enough space", "is a genuinely valuable initiative",
        ],
        "context": [
            "this semester", "for the whole campus", "for final year students",
            "during the annual fest", "on weekends",
        ],
    },
}

# Sentence frames the pieces are assembled into.
FRAMES: list[str] = [
    "{subject} {predicate} {context}.",
    "{subject} {context} {predicate}.",
    "I would like to report that {subject} {predicate} {context}.",
    "Requesting the department to look into this: {subject} {predicate} {context}.",
    "{subject} {predicate}. This has been the case {context}.",
    "Several students have noticed that {subject} {predicate} {context}.",
]

EXAMPLES_PER_CATEGORY = 90

# Filler that appears in real tickets regardless of subject. Mixing it in stops
# the categories being trivially separable and forces the model to rely on
# domain vocabulary rather than on frame structure.
NOISE_PHRASES: list[str] = [
    "Hoping for a quick response.", "This has been going on for a while.",
    "Thanks in advance.", "Please look into it at the earliest.",
    "I have already raised this verbally.", "Many of us are affected.",
    "Let me know if more details are needed.", "Attaching a photo for reference.",
    "Sorry if this is the wrong department.", "It would really help if this is fixed.",
]

# Character-level corruptions, applied to a fraction of documents to mimic the
# typos that appear in real submissions.
_TYPO_SWAPS = {"a": "", "e": "", "i": "", "o": "", "t": "tt", "l": "ll", "s": "ss"}


def _add_typos(text: str, rng: random.Random) -> str:
    """Corrupt one word so the character n-gram features have to earn their place."""
    words = text.split()
    if len(words) < 4:
        return text
    index = rng.randrange(len(words))
    word = words[index]
    for source, replacement in _TYPO_SWAPS.items():
        if source in word and len(word) > 4:
            words[index] = word.replace(source, replacement, 1)
            break
    return " ".join(words)


def build_corpus(examples_per_category: int = EXAMPLES_PER_CATEGORY,
                 seed: int = SEED,
                 augment: bool = True) -> tuple[list[str], list[str]]:
    """Generate the labelled training corpus.

    Args:
        examples_per_category: How many documents to synthesise per label.
        seed: RNG seed; fixed by default so training is reproducible.
        augment: Mix in filler phrases and typos. Disable to inspect the raw
            templates.

    Returns:
        A ``(texts, labels)`` pair of equal-length lists.
    """
    rng = random.Random(seed)
    texts: list[str] = []
    labels: list[str] = []

    for category, parts in TEMPLATES.items():
        seen: set[str] = set()
        attempts = 0
        while len(seen) < examples_per_category and attempts < examples_per_category * 50:
            attempts += 1
            sentence = rng.choice(FRAMES).format(
                subject=rng.choice(parts["subject"]),
                predicate=rng.choice(parts["predicate"]),
                context=rng.choice(parts["context"]),
            )
            # Capitalise, and collapse the double space some frames produce.
            sentence = " ".join(sentence.split())
            sentence = sentence[0].upper() + sentence[1:]

            if augment:
                if rng.random() < 0.45:
                    sentence = f"{sentence} {rng.choice(NOISE_PHRASES)}"
                if rng.random() < 0.20:
                    sentence = _add_typos(sentence, rng)

            if sentence in seen:
                continue
            seen.add(sentence)

        texts.extend(sorted(seen))
        labels.extend([category] * len(seen))

    return texts, labels


# --------------------------------------------------------------------------
# Held-out evaluation set.
#
# Hand-written, phrased the way students actually write, and never used for
# training. Cross-validated accuracy on a template-generated corpus flatters
# the model; this is the number worth quoting.
# --------------------------------------------------------------------------

EVALUATION_SET: list[tuple[str, str]] = [
    ("The dinner in the mess was undercooked again and three of us fell ill", "HOSTEL"),
    ("No water in block B bathrooms between 1pm and 5pm every single day", "HOSTEL"),
    ("Warden refuses to allow late entry even with a lab permission slip", "HOSTEL"),
    ("Revaluation marks for DS paper still not out after seven weeks", "EXAMINATION"),
    ("My internal assessment scores are missing for three subjects on the portal", "EXAMINATION"),
    ("Two papers scheduled on the same morning in the timetable", "EXAMINATION"),
    ("Wifi drops out constantly on the 4th floor during online quizzes", "IT_SUPPORT"),
    ("Lab PCs take 10 minutes to boot and the IDE keeps freezing", "IT_SUPPORT"),
    ("Cannot log into the student portal, it rejects my correct password", "IT_SUPPORT"),
    ("Prof explains signals and systems beautifully, best lectures this year", "FACULTY"),
    ("Lecturer has skipped four classes and never answers doubts", "FACULTY"),
    ("The lab instructor is very patient and helpful with beginners", "FACULTY"),
    ("Reading room shuts at 8pm which is useless during exam week", "LIBRARY"),
    ("IEEE journal access does not work from home, only on campus", "LIBRARY"),
    ("Great new algorithms books added to the reference section", "LIBRARY"),
    ("Route 7 bus is 25 minutes late every morning, we miss first period", "TRANSPORT"),
    ("The evening shuttle was cancelled without telling anyone", "TRANSPORT"),
    ("Scholarship money approved in March still has not been credited", "ADMINISTRATION"),
    ("Fee receipt shows 8000 more than the notified fee structure", "ADMINISTRATION"),
    ("Bonafide certificate took five visits to the office to obtain", "ADMINISTRATION"),
    ("Three ceiling fans dead in room 204 and six chairs are broken", "INFRASTRUCTURE"),
    ("Lathe machines in the workshop are missing their chip guards", "INFRASTRUCTURE"),
    ("Projector in seminar hall 2 has not worked for four sessions", "INFRASTRUCTURE"),
    ("Syllabus for sixth semester is far too heavy for the time given", "ACADEMIC"),
    ("Placement training clashes with the database lab every week", "ACADEMIC"),
    ("Please arrange extra tutorial hours for the python module", "ACADEMIC"),
    ("Sports ground is locked most evenings and nobody knows who has the key", "OTHER"),
    ("The annual fest was organised really well this year", "OTHER"),
]


def evaluation_set() -> tuple[list[str], list[str]]:
    """Hand-written held-out documents.

    Returns:
        A ``(texts, labels)`` pair, disjoint from :func:`build_corpus`.
    """
    return [text for text, _ in EVALUATION_SET], [label for _, label in EVALUATION_SET]


def category_names() -> list[str]:
    """Labels the classifier is trained to predict."""
    return sorted(TEMPLATES.keys())
