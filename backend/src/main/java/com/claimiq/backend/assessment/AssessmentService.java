package com.claimiq.backend.assessment;

import com.claimiq.backend.claims.dto.ClaimResponse;
import com.claimiq.backend.claims.exception.ClaimVersionConflictException;
import com.claimiq.backend.claims.service.ClaimService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AssessmentService {
    private final ClaimService claims;
    private final AssessmentClient client;

    public AssessmentService(ClaimService claims, AssessmentClient client) {
        this.claims = claims;
        this.client = client;
    }

    public record Response(UUID claimId, long claimVersion, AssessmentResult assessment) {}

    public Response assess(UUID id, long expectedVersion) {
        // Check authorization and the saved version before sending any indicators.
        ClaimResponse claim = claims.get(id);
        if (claim.version() != expectedVersion) throw new ClaimVersionConflictException();
        if (claim.context() == null) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(422),
                    "Complete insurance, diagnoses and billed services before assessment");
        }
        AssessmentResult result = client.assess(claim.context());
        if (claims.get(id).version() != claim.version()) throw new ClaimVersionConflictException();
        return new Response(id, claim.version(), result);
    }
}
