package com.claimiq.backend.assessment;

import java.util.List;

/** Log-odds contributions are model explanations, not causal attribution. */
public record AssessmentResult(
        String modelVersion, String trainingData, String assessedAt,
        double score, String riskLevel, double interceptLogOdds,
        List<Finding> findings, List<String> limitations) {
    public record Finding(String code, String title, String field, String evidence,
                          double contributionLogOdds, String suggestedCorrection) {}
}
