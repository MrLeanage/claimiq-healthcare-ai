package com.claimiq.backend.claims.api;

import com.claimiq.backend.claims.dto.ClaimRequest;
import com.claimiq.backend.claims.dto.ClaimResponse;
import com.claimiq.backend.claims.dto.StatusRequest;
import com.claimiq.backend.claims.service.ClaimService;

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

    public ClaimController(ClaimService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ClaimResponse> create(@Valid @RequestBody ClaimRequest request) {
        ClaimResponse result = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/claims/" + result.id()))
                .eTag(Long.toString(result.version()))
                .body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponse> get(@PathVariable UUID id) {
        return response(service.get(id));
    }

    @GetMapping
    public Page<ClaimResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new IllegalArgumentException("page must be >= 0 and size between 1 and 100");
        return service.list(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClaimResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ClaimRequest request,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        return response(service.update(id, request, VersionPrecondition.parse(ifMatch)));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ClaimResponse> submit(
            @PathVariable UUID id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        return response(service.submit(id, VersionPrecondition.parse(ifMatch)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ClaimResponse> transition(
            @PathVariable UUID id,
            @Valid @RequestBody StatusRequest request,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        return response(
                service.transition(id, request.status(), VersionPrecondition.parse(ifMatch)));
    }

    private ResponseEntity<ClaimResponse> response(ClaimResponse result) {
        return ResponseEntity.ok().eTag(Long.toString(result.version())).body(result);
    }
}
