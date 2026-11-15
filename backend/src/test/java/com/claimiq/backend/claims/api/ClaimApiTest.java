package com.claimiq.backend.claims.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.claimiq.backend.claims.domain.ClaimStatus;
import com.claimiq.backend.claims.dto.ClaimResponse;
import com.claimiq.backend.claims.exception.ClaimNotFoundException;
import com.claimiq.backend.claims.exception.InvalidClaimTransitionException;
import com.claimiq.backend.claims.service.ClaimService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

class ClaimApiTest {
    private final ClaimService service = mock(ClaimService.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc =
                MockMvcBuilders.standaloneSetup(new ClaimController(service))
                        .setControllerAdvice(new ApiExceptionHandler())
                        .build();
    }

    @Test
    void missingClaimUsesTheStableErrorContract() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenThrow(new ClaimNotFoundException(id));
        assertError(
                mvc.perform(get("/api/v1/claims/{id}", id)).andReturn(),
                404,
                "CLAIM_NOT_FOUND",
                "Claim not found: " + id);
    }

    @Test
    void invalidTransitionRemainsAConflict() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.transition(id, ClaimStatus.APPROVED, 0))
                .thenThrow(new InvalidClaimTransitionException("Invalid transition"));
        assertError(
                mvc.perform(
                                patch("/api/v1/claims/{id}/status", id)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"status\":\"APPROVED\"}")
                                        .header("If-Match", "\"0\""))
                        .andReturn(),
                409,
                "INVALID_CLAIM_TRANSITION",
                "Invalid transition");
    }

    @Test
    void invalidCreateRequestNeverReachesTheService() throws Exception {
        assertError(
                mvc.perform(
                                post("/api/v1/claims")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                        .andReturn(),
                400,
                "INVALID_REQUEST",
                "Request validation failed");
        verifyNoInteractions(service);
    }

    @Test
    void malformedJsonNeverReachesTheService() throws Exception {
        assertError(
                mvc.perform(
                                post("/api/v1/claims")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{"))
                        .andReturn(),
                400,
                "INVALID_REQUEST",
                "Request validation failed");
        verifyNoInteractions(service);
    }

    @Test
    void invalidPaginationNeverReachesTheService() throws Exception {
        assertError(
                mvc.perform(get("/api/v1/claims?page=-1&size=20")).andReturn(),
                400,
                "INVALID_REQUEST",
                "Request validation failed");
        verifyNoInteractions(service);
    }

    @Test
    void createPreservesCreatedStatusAndLocation() throws Exception {
        UUID id = UUID.randomUUID();
        ClaimResponse response =
                new ClaimResponse(
                        id,
                        "CLM-DEMO",
                        "PAT-DEMO",
                        "PROV-DEMO",
                        null,
                        "USD",
                        null,
                        ClaimStatus.DRAFT,
                        null,
                        null,
                        null,
                        0);
        when(service.create(any())).thenReturn(response);
        MvcResult result =
                mvc.perform(
                                post("/api/v1/claims")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
{"patientReference":"PAT-DEMO","providerReference":"PROV-DEMO",
 "amount":125.50,"currency":"USD","serviceDate":"2026-01-01"}
"""))
                        .andReturn();
        assertEquals(201, result.getResponse().getStatus());
        assertEquals("/api/v1/claims/" + id, result.getResponse().getHeader("Location"));
        assertEquals(
                id.toString(),
                json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    void optimisticLockFailuresUseAPreconditionResponse() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.submit(id, 0))
                .thenThrow(
                        new org.springframework.orm.ObjectOptimisticLockingFailureException(
                                "Claim", id));
        assertError(
                mvc.perform(post("/api/v1/claims/{id}/submit", id).header("If-Match", "\"0\""))
                        .andReturn(),
                412,
                "STALE_CLAIM_VERSION",
                "Claim has changed; reload it before retrying");
    }

    private void assertError(MvcResult result, int status, String code, String message)
            throws Exception {
        assertEquals(status, result.getResponse().getStatus());
        var body = json.readTree(result.getResponse().getContentAsString());
        assertEquals(4, body.size());
        assertEquals(status, body.get("status").asInt());
        assertEquals(code, body.get("code").asText());
        assertEquals(message, body.get("message").asText());
        assertNotNull(Instant.parse(body.get("timestamp").asText()));
    }
}
