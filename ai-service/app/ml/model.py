"""Small, dependency-free logistic regression; all training outcomes are synthetic."""

import json
import math
from pathlib import Path

MODEL_PATH = Path(__file__).with_name("synthetic-model.json")
FEATURES = (
    "coverage_inactive",
    "coverage_unknown",
    "coverage_unverified",
    "authorization_denied",
    "authorization_pending",
    "authorization_unknown",
    "local_codes",
    "summary_missing",
)


def sigmoid(value):
    return 1 / (1 + math.exp(-max(-40, min(40, value))))


def load_model():
    model = json.loads(MODEL_PATH.read_text(encoding="utf-8"))
    if model["features"] != list(FEATURES) or model["trainingData"] != "SYNTHETIC":
        raise ValueError("Incompatible model artifact")
    return model


def probability(model, features):
    return sigmoid(
        model["intercept"] + sum(w * x for w, x in zip(model["weights"], features))
    )
