package com.claimiq.backend.claims;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return ResponseEntity.created(URI.create("/api/v1/claims/" + result.id())).body(result);
    }

    @GetMapping("/{id}")
    public ClaimResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    public Page<ClaimResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new IllegalArgumentException("page must be >= 0 and size between 1 and 100");
        return service.list(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PutMapping("/{id}")
    public ClaimResponse update(@PathVariable UUID id, @Valid @RequestBody ClaimRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/submit")
    public ClaimResponse submit(@PathVariable UUID id) {
        return service.submit(id);
    }

    @PatchMapping("/{id}/status")
    public ClaimResponse transition(
            @PathVariable UUID id, @Valid @RequestBody StatusRequest request) {
        return service.transition(id, request.status());
    }

    public record StatusRequest(@jakarta.validation.constraints.NotNull ClaimStatus status) {}
}
