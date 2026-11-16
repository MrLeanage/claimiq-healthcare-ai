package com.claimiq.backend.assessment;

import com.claimiq.backend.claims.dto.ClaimContextDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class HttpAssessmentClient implements AssessmentClient {
    private static final Logger log = LoggerFactory.getLogger(HttpAssessmentClient.class);
    private final RestClient client;

    public HttpAssessmentClient(
            @Value("${claimiq.ai.base-url:http://127.0.0.1:8090}") String baseUrl,
            @Value("${claimiq.ai.api-key:}") String apiKey) {
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        client =
                RestClient.builder()
                        .baseUrl(baseUrl)
                        .requestFactory(factory)
                        .defaultHeader("X-AI-Key", apiKey)
                        .build();
    }

    @Override
    public AssessmentResult assess(ClaimContextDto context) {
        try {
            AssessmentResult result =
                    client.post()
                            .uri("/assess")
                            .body(Map.of("context", indicators(context)))
                            .retrieve()
                            .body(AssessmentResult.class);
            if (result == null) throw new IllegalStateException("Empty assessment response");
            return result;
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            throw unavailable(
                    status == 401 || status == 403 ? "AUTHENTICATION" : "HTTP_STATUS", ex, status);
        } catch (ResourceAccessException ex) {
            throw unavailable("TRANSPORT", ex, null);
        } catch (RestClientException | IllegalStateException ex) {
            throw unavailable("CONTRACT", ex, null);
        }
    }

    private ResponseStatusException unavailable(
            String category, Exception failure, Integer status) {
        String reference = UUID.randomUUID().toString();
        // Dependency exception messages/bodies may contain claim data or service keys.
        log.warn(
                "Assessment failed reference={} category={} exception={} upstreamStatus={}",
                reference,
                category,
                failure.getClass().getSimpleName(),
                status);
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Assessment unavailable. Retry or contact support with reference " + reference);
    }

    private Map<String, Object> indicators(ClaimContextDto context) {
        var insurance = context.insurance();
        return Map.of(
                "insurance",
                        Map.of(
                                "coverageStatus", insurance.coverageStatus(),
                                "coverageVerifiedOn",
                                        insurance.coverageVerifiedOn() == null
                                                ? ""
                                                : insurance.coverageVerifiedOn().toString(),
                                "authorizationStatus", insurance.authorizationStatus()),
                "diagnosisCodes", List.of("PRESENT"),
                "serviceLines",
                        context.serviceLines().stream()
                                .map(line -> Map.of("codeSystem", line.codeSystem()))
                                .toList(),
                "clinicalSummary",
                        context.clinicalSummary() == null || context.clinicalSummary().isBlank()
                                ? ""
                                : "PRESENT");
    }
}
