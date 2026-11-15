package com.claimiq.backend.claims.api;

import com.claimiq.backend.assessment.AssessmentService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/claims")
public class AssessmentController {
    private final AssessmentService assessments;

    public AssessmentController(AssessmentService assessments) {
        this.assessments = assessments;
    }

    @PostMapping("/{id}/assessment")
    public ResponseEntity<AssessmentService.Response> assess(@PathVariable UUID id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(assessments.assess(id, VersionPrecondition.parse(ifMatch)));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<com.claimiq.backend.claims.dto.ApiError> unavailable(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore())
                .body(new com.claimiq.backend.claims.dto.ApiError(java.time.Instant.now().toString(),
                        error.getStatusCode().value(), "ASSESSMENT_UNAVAILABLE", error.getReason()));
    }
}
