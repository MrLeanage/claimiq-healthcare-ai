package com.claimiq.backend.assessment;

import com.claimiq.backend.claims.dto.ClaimResponse;
import com.claimiq.backend.claims.dto.ClaimContextDto;
import com.claimiq.backend.claims.exception.ClaimVersionConflictException;
import com.claimiq.backend.claims.service.ClaimService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AssessmentService {
    private final ClaimService claims;
    private final RestClient client;

    public AssessmentService(
            ClaimService claims,
            @Value("${claimiq.ai.base-url:http://127.0.0.1:8090}") String baseUrl,
            @Value("${claimiq.ai.api-key:}") String apiKey) {
        this.claims = claims;
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("X-AI-Key", apiKey)
                .build();
    }

    public record Response(UUID claimId, long claimVersion, AssessmentResult assessment) {}

    public Response assess(UUID id, long expectedVersion) {
        // Reuse ownership and scope checks before making any external request.
        ClaimResponse claim = claims.get(id);
        if (claim.version() != expectedVersion) throw new ClaimVersionConflictException();
        if (claim.context() == null) {
            throw new ResponseStatusException(HttpStatus.valueOf(422),
                    "Complete insurance, diagnoses and billed services before assessment");
        }
        AssessmentResult result;
        try {
            result = client.post()
                    .uri("/assess")
                    .body(Map.of("context", indicators(claim.context())))
                    .retrieve()
                    .body(AssessmentResult.class);
            if (result == null || !"SYNTHETIC".equals(result.trainingData())
                    || !Double.isFinite(result.score()) || result.score() < 0 || result.score() > 100
                    || result.findings() == null || result.limitations() == null
                    || result.riskLevel() == null
                    || !Set.of("LOW", "MEDIUM", "HIGH").contains(result.riskLevel())) {
                throw new IllegalStateException("Invalid assessment response");
            }
        } catch (RestClientException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Assessment service unavailable. Start the AI service and try again.");
        }
        // Discard a result if the saved claim changed while inference was running.
        if (claims.get(id).version() != claim.version()) throw new ClaimVersionConflictException();
        return new Response(id, claim.version(), result);
    }

    private Map<String, Object> indicators(ClaimContextDto context) {
        // Send presence indicators, never references, code identifiers or clinical text.
        var insurance = context.insurance();
        return Map.of(
                "insurance", Map.of(
                        "coverageStatus", insurance.coverageStatus(),
                        "coverageVerifiedOn", insurance.coverageVerifiedOn() == null
                                ? "" : insurance.coverageVerifiedOn().toString(),
                        "authorizationStatus", insurance.authorizationStatus()),
                "diagnosisCodes", List.of("PRESENT"),
                "serviceLines", context.serviceLines().stream()
                        .map(line -> Map.of("codeSystem", line.codeSystem())).toList(),
                "clinicalSummary", context.clinicalSummary() == null
                                || context.clinicalSummary().isBlank() ? "" : "PRESENT");
    }
}
