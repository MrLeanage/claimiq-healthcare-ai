"""Reproduce the demonstration artifact. No patient data or external dependencies."""

import json
import random
from app.ml.model import FEATURES, MODEL_PATH, probability, sigmoid


def train():
    rng = random.Random(2718)
    # Invented data-generating assumptions, NOT evidence about payer behavior.
    assumptions = [3.1, 1.4, 0.8, 3.0, 1.8, 1.1, 0.7, 0.5]
    rows = []
    for _ in range(4000):
        coverage = rng.choice([0, 0, 0, 1, 2])
        authorization = rng.choice([0, 0, 0, 1, 2, 3])
        x = [
            int(coverage == 1),
            int(coverage == 2),
            rng.randrange(2),
            int(authorization == 1),
            int(authorization == 2),
            int(authorization == 3),
            rng.randrange(2),
            rng.randrange(2),
        ]
        y = int(
            rng.random() < sigmoid(-3.2 + sum(a * b for a, b in zip(assumptions, x)))
        )
        rows.append((x, y))
    training, held_out = rows[:3200], rows[3200:]
    model = dict(
        features=list(FEATURES),
        weights=[0.0] * len(FEATURES),
        intercept=0.0,
        version="synthetic-logistic-v1",
        trainingData="SYNTHETIC",
        seed=2718,
        algorithm="logistic regression",
        trainingRows=len(training),
        testRows=len(held_out),
        generatorIntercept=-3.2,
        generatorWeights=assumptions,
    )
    for _ in range(700):
        errors = [(x, probability(model, x) - y) for x, y in training]
        model["intercept"] -= 0.5 * sum(e for _, e in errors) / len(training)
        for j in range(len(FEATURES)):
            gradient = sum(e * x[j] for x, e in errors) / len(training)
            model["weights"][j] -= 0.5 * (gradient + 0.001 * model["weights"][j])
    model["syntheticTestMetrics"] = {
        "accuracyAtThreshold05": sum(
            (probability(model, x) >= 0.5) == bool(y) for x, y in held_out
        )
        / len(held_out),
        "brierScore": sum((probability(model, x) - y) ** 2 for x, y in held_out)
        / len(held_out),
        "warning": "Measures fit to invented synthetic outcomes only; not clinical or payer validation.",
    }
    MODEL_PATH.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(model["syntheticTestMetrics"], indent=2))


if __name__ == "__main__":
    train()
