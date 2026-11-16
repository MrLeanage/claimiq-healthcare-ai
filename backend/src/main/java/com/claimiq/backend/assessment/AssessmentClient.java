package com.claimiq.backend.assessment;

import com.claimiq.backend.claims.dto.ClaimContextDto;

/** External inference boundary. Implementations must return a validated result. */
public interface AssessmentClient {
    AssessmentResult assess(ClaimContextDto context);
}
