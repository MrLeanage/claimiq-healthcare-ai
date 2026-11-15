import copy
import math
import unittest
from app.ml.model import load_model
from app.services.assessment import assess


class AssessmentTests(unittest.TestCase):
    def setUp(self):
        self.model = load_model()
        self.claim = {
            "context": {
                "insurance": {
                    "coverageStatus": "ACTIVE",
                    "coverageVerifiedOn": "2026-11-01",
                    "authorizationStatus": "NOT_REQUIRED",
                },
                "diagnosisCodes": ["DEMO"],
                "serviceLines": [{"codeSystem": "CPT"}],
                "clinicalSummary": "PRESENT",
            }
        }

    def test_risk_rises_with_authorization_problem_and_has_specific_correction(self):
        clean = assess(self.claim, self.model)
        self.claim["context"]["insurance"]["authorizationStatus"] = "DENIED"
        risky = assess(self.claim, self.model)
        self.assertGreater(risky["score"], clean["score"])
        self.assertEqual("authorization_denied", risky["findings"][0]["code"])
        self.assertIn(
            "payer authorization response", risky["findings"][0]["suggestedCorrection"]
        )
        self.assertEqual("SYNTHETIC", risky["trainingData"])

    def test_explanations_reconstruct_the_score(self):
        self.claim["context"]["insurance"]["coverageStatus"] = "INACTIVE"
        self.claim["context"]["insurance"]["authorizationStatus"] = "PENDING"
        result = assess(self.claim, self.model)
        logit = result["interceptLogOdds"] + sum(
            f["contributionLogOdds"] for f in result["findings"]
        )
        self.assertEqual(result["score"], round(100 / (1 + math.exp(-logit)), 1))

    def test_references_and_free_text_do_not_affect_score_or_leave_in_findings(self):
        expected = assess(self.claim, self.model)["score"]
        self.claim["patientReference"] = "SECRET"
        self.claim["context"]["clinicalSummary"] = "SECRET CLINICAL TEXT"
        result = assess(self.claim, self.model)
        self.assertEqual(expected, result["score"])
        self.assertNotIn("SECRET", str(result))

    def test_no_automatic_mutation_and_incomplete_input_refused(self):
        original = copy.deepcopy(self.claim)
        assess(self.claim, self.model)
        self.assertEqual(original, self.claim)
        for payload in ({}, {"context": None}, {"context": {"insurance": {}}}):
            with self.assertRaises(ValueError):
                assess(payload, self.model)

    def test_unknown_is_distinct_from_inactive_and_not_required(self):
        self.claim["context"]["insurance"]["coverageStatus"] = "UNKNOWN"
        result = assess(self.claim, self.model)
        self.assertEqual(["coverage_unknown"], [f["code"] for f in result["findings"]])


if __name__ == "__main__":
    unittest.main()
