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
    validate_model(model)
    return model


def validate_model(model):
    if (
        not isinstance(model, dict)
        or model.get("features") != list(FEATURES)
        or model.get("trainingData") != "SYNTHETIC"
        or not isinstance(model.get("version"), str)
        or not model["version"].strip()
        or not isinstance(model.get("weights"), list)
        or len(model["weights"]) != len(FEATURES)
        or not all(_finite_number(weight) for weight in model["weights"])
        or not _finite_number(model.get("intercept"))
    ):
        raise ValueError("Incompatible model artifact")


def _finite_number(value):
    # bool is an int subclass, but is not a model parameter.
    try:
        return type(value) in (int, float) and math.isfinite(value)
    except OverflowError:
        return False


def probability(model, features):
    validate_model(model)
    if len(features) != len(FEATURES) or any(
        type(value) is not int or value not in (0, 1) for value in features
    ):
        raise ValueError("Expected eight binary model features")
    return sigmoid(
        model["intercept"]
        + sum(w * x for w, x in zip(model["weights"], features, strict=True))
    )
