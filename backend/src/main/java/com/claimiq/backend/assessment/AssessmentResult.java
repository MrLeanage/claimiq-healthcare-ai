package com.claimiq.backend.assessment;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/** Log-odds contributions are model explanations, not causal attribution. */
public record AssessmentResult(
        String modelVersion,
        String trainingData,
        String assessedAt,
        Double score,
        String riskLevel,
        Double interceptLogOdds,
        List<Finding> findings,
        List<String> limitations) {
    // Wrapper Doubles distinguish missing JSON values from a real zero score/contribution.
    // Constructor invariants reject malformed upstream data before it reaches React.
    public AssessmentResult {
        requireText(modelVersion);
        if (!"SYNTHETIC".equals(trainingData)
                || score == null
                || !Double.isFinite(score)
                || score < 0
                || score > 100
                || riskLevel == null
                || !Set.of("LOW", "MEDIUM", "HIGH").contains(riskLevel)
                || interceptLogOdds == null
                || !Double.isFinite(interceptLogOdds)
                || findings == null
                || findings.stream().anyMatch(item -> item == null)
                || limitations == null
                || limitations.isEmpty()) {
            throw new IllegalArgumentException("Invalid assessment response");
        }
        requireText(assessedAt);
        OffsetDateTime.parse(assessedAt);
        limitations.forEach(AssessmentResult::requireText);
        findings = List.copyOf(findings);
        limitations = List.copyOf(limitations);
    }

    public record Finding(
            String code,
            String title,
            String field,
            String evidence,
            Double contributionLogOdds,
            String suggestedCorrection) {
        public Finding {
            requireText(code);
            requireText(title);
            requireText(field);
            requireText(evidence);
            requireText(suggestedCorrection);
            if (contributionLogOdds == null || !Double.isFinite(contributionLogOdds)) {
                throw new IllegalArgumentException("Invalid assessment contribution");
            }
        }
    }

    private static void requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing assessment text");
        }
    }
}
