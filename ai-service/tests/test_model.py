import copy
import json
import math
import unittest
from unittest.mock import patch

from app.ml.model import load_model, probability


class ModelContractTests(unittest.TestCase):
    def setUp(self):
        self.model = load_model()

    def test_loader_rejects_invalid_artifacts(self):
        invalid_fields = (
            ("weights", []),
            ("weights", self.model["weights"] + [1.0]),
            ("weights", [math.nan] * 8),
            ("weights", [True] * 8),
            ("weights", ["1"] * 8),
            ("intercept", math.inf),
            ("intercept", 10**400),
            ("intercept", None),
            ("version", " "),
            ("features", list(reversed(self.model["features"]))),
            ("trainingData", "REAL"),
        )
        for field, value in invalid_fields:
            with self.subTest(field=field, value=value):
                artifact = copy.deepcopy(self.model)
                artifact[field] = value
                with patch("app.ml.model.MODEL_PATH") as path:
                    path.read_text.return_value = json.dumps(artifact)
                    with self.assertRaises(ValueError):
                        load_model()

    def test_inference_rejects_invalid_feature_vectors(self):
        for values in ([0] * 7, [0] * 9, [2] * 8, [math.nan] * 8, [True] * 8):
            with self.subTest(values=values), self.assertRaises(ValueError):
                probability(self.model, values)

    def test_inference_rejects_mutated_weights(self):
        self.model["weights"] = []
        with self.assertRaises(ValueError):
            probability(self.model, [1] * 8)
