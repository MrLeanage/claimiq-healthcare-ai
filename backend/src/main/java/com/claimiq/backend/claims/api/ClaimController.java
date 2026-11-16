package com.claimiq.backend.claims.api;

import com.claimiq.backend.claims.dto.ClaimRequest;
import com.claimiq.backend.claims.dto.ClaimResponse;
import com.claimiq.backend.claims.dto.ClaimSummary;
import com.claimiq.backend.claims.dto.StatusRequest;
import com.claimiq.backend.claims.service.ClaimCreationService;
import com.claimiq.backend.claims.service.ClaimService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import jakarta.validation.Valid;

import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {
    private final ClaimService service;
    private final ClaimCreationService creation;

    public ClaimController(ClaimService service, ClaimCreationService creation) {
        this.service = service;
        this.creation = creation;
    }

    @Operation(summary = "Create a draft claim (claims:write)")
    @PostMapping
    public ResponseEntity<ClaimResponse> create(
            @Valid @RequestBody ClaimRequest request,
            @Parameter(description = "Reuse the same UUID and payload when retrying creation")
                    @RequestHeader(value = "Idempotency-Key", required = false)
                    UUID key) {
        ClaimResponse result = creation.create(request, key);
        return ResponseEntity.created(URI.create("/api/v1/claims/" + result.id()))
                .eTag(Long.toString(result.version()))
                .body(result);
    }

    @Operation(summary = "Read a claim and its ETag (claims:read)")
    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponse> get(@PathVariable UUID id) {
        return response(service.get(id));
    }

    @Operation(summary = "List visible claims (claims:read)")
    @GetMapping
    public Page<ClaimSummary> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new IllegalArgumentException("page must be >= 0 and size between 1 and 100");
        return service.list(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @Operation(summary = "Update a draft claim (claims:write)")
    @PutMapping("/{id}")
    public ResponseEntity<ClaimResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ClaimRequest request,
            @Parameter(
                            required = true,
                            description = "Latest quoted ETag returned by the API",
                            example = "\"0\"")
                    @RequestHeader(value = "If-Match", required = false)
                    String ifMatch) {
        return response(service.update(id, request, VersionPrecondition.parse(ifMatch)));
    }

    @Operation(summary = "Submit a draft claim (claims:write)")
    @PostMapping("/{id}/submit")
    public ResponseEntity<ClaimResponse> submit(
            @PathVariable UUID id,
            @Parameter(
                            required = true,
                            description = "Latest quoted ETag returned by the API",
                            example = "\"0\"")
                    @RequestHeader(value = "If-Match", required = false)
                    String ifMatch) {
        return response(service.submit(id, VersionPrecondition.parse(ifMatch)));
    }

    @Operation(summary = "Review a submitted claim (claims:review)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ClaimResponse> transition(
            @PathVariable UUID id,
            @Valid @RequestBody StatusRequest request,
            @Parameter(
                            required = true,
                            description = "Latest quoted ETag returned by the API",
                            example = "\"0\"")
                    @RequestHeader(value = "If-Match", required = false)
                    String ifMatch) {
        return response(
                service.transition(id, request.status(), VersionPrecondition.parse(ifMatch)));
    }

    private ResponseEntity<ClaimResponse> response(ClaimResponse result) {
        return ResponseEntity.ok().eTag(Long.toString(result.version())).body(result);
    }
}
