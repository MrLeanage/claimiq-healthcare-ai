package com.claimiq.backend.claims.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.claimiq.backend.claims.repository.ClaimAuditRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired ClaimAuditRepository audit;
    private final JsonMapper json = JsonMapper.builder().build();
    private static final String REQUEST =
            """
{"patientReference":"PAT-DEMO","providerReference":"PROV-DEMO",
 "amount":125.50,"currency":"USD","serviceDate":"2026-01-01","context":{"insurance":{"payerName":"Demo payer","planReference":"PLAN-DEMO","memberReference":"MEM-DEMO","coverageStatus":"UNKNOWN","coverageVerifiedOn":null,"authorizationStatus":"NOT_REQUIRED","authorizationReference":null},"diagnosisCodes":["Z00.00"],"serviceLines":[{"codeSystem":"LOCAL","procedureCode":"DEMO-001","description":"Synthetic consultation","units":1,"unitPrice":125.5}],"clinicalSummary":"Synthetic clinical context"}}
""";

    private JwtRequestPostProcessor user(String name, String... scopes) {
        return jwt().jwt(token -> token.subject(name))
                .authorities(
                        java.util.Arrays.stream(scopes)
                                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                                .toArray(
                                        org.springframework.security.core.GrantedAuthority[]::new));
    }

    @Test
    void mismatchedServiceTotalsAreRejectedByTheApi() throws Exception {
        assertEquals(
                400,
                mvc.perform(
                                post("/api/v1/claims")
                                        .with(user("mismatch-writer", "claims:write"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                REQUEST.replace(
                                                        "\"amount\":125.50", "\"amount\":125.49")))
                        .andReturn()
                        .getResponse()
                        .getStatus());
    }

    @Test
    void apiDocumentationIsUnavailableByDefault() throws Exception {
        assertEquals(401, mvc.perform(get("/api-docs")).andReturn().getResponse().getStatus());
        assertEquals(
                404,
                mvc.perform(get("/api-docs").with(user("reader", "claims:read")))
                        .andReturn()
                        .getResponse()
                        .getStatus());
        assertEquals(
                401,
                mvc.perform(get("/swagger-ui/index.html")).andReturn().getResponse().getStatus());
    }

    @Test
    void unauthenticatedAndUnconfiguredBearerTokensCannotAccessClaims() throws Exception {
        assertEquals(401, mvc.perform(get("/api/v1/claims")).andReturn().getResponse().getStatus());
        assertEquals(
                401,
                mvc.perform(get("/api/v1/claims").header("Authorization", "Bearer unsigned-token"))
                        .andReturn()
                        .getResponse()
                        .getStatus());
    }

    @Test
    void paddedIdentitySubjectsAreRejected() throws Exception {
        assertEquals(
                403,
                mvc.perform(get("/api/v1/claims").with(user("alice ", "claims:read")))
                        .andReturn()
                        .getResponse()
                        .getStatus());
    }

    @Test
    void scopesOwnershipVersionsAndAuditAreEnforced() throws Exception {
        String owner = "alice-" + UUID.randomUUID();
        var created =
                mvc.perform(
                                post("/api/v1/claims")
                                        .with(user(owner, "claims:write"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(REQUEST))
                        .andReturn()
                        .getResponse();
        assertEquals(201, created.getStatus());
        UUID id = UUID.fromString(json.readTree(created.getContentAsString()).get("id").asText());
        String path = "/api/v1/claims/" + id;
        String tag = created.getHeader("ETag");
        assertEquals("\"0\"", tag);

        assertEquals(
                404,
                mvc.perform(get(path).with(user("bob", "claims:read")))
                        .andReturn()
                        .getResponse()
                        .getStatus());
        assertEquals(
                404,
                mvc.perform(
                                put(path)
                                        .with(user("bob", "claims:write"))
                                        .header("If-Match", tag)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(REQUEST))
                        .andReturn()
                        .getResponse()
                        .getStatus());
        var bobList =
                mvc.perform(get("/api/v1/claims").with(user("bob", "claims:read")))
                        .andReturn()
                        .getResponse();
        assertEquals(0, json.readTree(bobList.getContentAsString()).get("totalElements").asInt());
        assertEquals(
                403,
                mvc.perform(
                                post("/api/v1/claims")
                                        .with(user("reader", "claims:read"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(REQUEST))
                        .andReturn()
                        .getResponse()
                        .getStatus());
        assertEquals(
                428,
                mvc.perform(post(path + "/submit").with(user(owner, "claims:write")))
                        .andReturn()
                        .getResponse()
                        .getStatus());

        var submitted =
                mvc.perform(
                                post(path + "/submit")
                                        .with(user(owner, "claims:write"))
                                        .header("If-Match", tag))
                        .andReturn()
                        .getResponse();
        assertEquals(200, submitted.getStatus());
        assertEquals("\"1\"", submitted.getHeader("ETag"));
        assertEquals(
                412,
                mvc.perform(
                                post(path + "/submit")
                                        .with(user(owner, "claims:write"))
                                        .header("If-Match", tag))
                        .andReturn()
                        .getResponse()
                        .getStatus());
        assertEquals(
                403,
                mvc.perform(
                                patch(path + "/status")
                                        .with(user(owner, "claims:write"))
                                        .header("If-Match", submitted.getHeader("ETag"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"status\":\"UNDER_REVIEW\"}"))
                        .andReturn()
                        .getResponse()
                        .getStatus());
        var reviewed =
                mvc.perform(
                                patch(path + "/status")
                                        .with(user("reviewer", "claims:review"))
                                        .header("If-Match", submitted.getHeader("ETag"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"status\":\"UNDER_REVIEW\"}"))
                        .andReturn()
                        .getResponse();
        assertEquals(200, reviewed.getStatus());
        assertEquals("\"2\"", reviewed.getHeader("ETag"));
        var events = audit.findAllByClaimIdOrderByOccurredAtAsc(id);
        assertEquals(3, events.size());
        assertEquals(owner, events.get(0).getActorSubject());
        assertEquals("reviewer", events.get(2).getActorSubject());
        assertEquals("TRANSITION", events.get(2).getAction());
        assertEquals(2, events.get(2).getClaimVersion());
    }

    @Test
    void keyedCreationReplaysOneClaimAndRejectsChangedPayload() throws Exception {
        String subject = "retry-" + UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        String id = createWithKey(subject, key, REQUEST);
        assertEquals(id, createWithKey(subject, key, REQUEST));
        var conflict =
                mvc.perform(
                                post("/api/v1/claims")
                                        .with(user(subject, "claims:write"))
                                        .header("Idempotency-Key", key)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(REQUEST.replace("PAT-DEMO", "PAT-CHANGED")))
                        .andReturn()
                        .getResponse();
        assertEquals(409, conflict.getStatus());
        assertEquals(
                "IDEMPOTENCY_CONFLICT",
                json.readTree(conflict.getContentAsString()).get("code").asText());
        assertEquals(1, audit.findAllByClaimIdOrderByOccurredAtAsc(UUID.fromString(id)).size());
        assertNotEquals(id, createWithKey(subject + "-other", key, REQUEST));
    }

    @Test
    void failedCreationRollsBackTheReservation() throws Exception {
        String subject = "rollback-" + UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        var invalid =
                mvc.perform(
                                post("/api/v1/claims")
                                        .with(user(subject, "claims:write"))
                                        .header("Idempotency-Key", key)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                REQUEST.replace(
                                                        "\"amount\":125.50", "\"amount\":125.49")))
                        .andReturn()
                        .getResponse();
        assertEquals(400, invalid.getStatus());
        assertNotNull(createWithKey(subject, key, REQUEST));
    }

    @Test
    void concurrentRetriesCreateOneClaimAndOneAuditEvent() throws Exception {
        String subject = "concurrent-" + UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        var start = new java.util.concurrent.CountDownLatch(1);
        var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<String> attempt =
                    () -> {
                        assertTrue(start.await(10, java.util.concurrent.TimeUnit.SECONDS));
                        return createWithKey(subject, key, REQUEST);
                    };
            var first = workers.submit(attempt);
            var second = workers.submit(attempt);
            start.countDown();
            String id = first.get(15, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(id, second.get(15, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1, audit.findAllByClaimIdOrderByOccurredAtAsc(UUID.fromString(id)).size());
        } finally {
            workers.shutdownNow();
        }
    }

    @Autowired jakarta.persistence.EntityManagerFactory entityManagers;

    @Test
    void listingDoesNotReturnOrLoadClinicalCollections() throws Exception {
        String subject = "summary-" + UUID.randomUUID();
        createWithKey(subject, UUID.randomUUID().toString(), REQUEST);
        var statistics = entityManagers.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            var response =
                    mvc.perform(get("/api/v1/claims").with(user(subject, "claims:read")))
                            .andReturn()
                            .getResponse();
            assertEquals(200, response.getStatus());
            var content = json.readTree(response.getContentAsString()).get("content");
            assertEquals(1, content.size());
            assertFalse(content.get(0).has("context"));
            assertFalse(response.getContentAsString().contains("MEM-DEMO"));
            assertEquals(0, statistics.getCollectionLoadCount());
            assertEquals(0, statistics.getEntityLoadCount());
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    private String createWithKey(String subject, String key, String body) throws Exception {
        var response =
                mvc.perform(
                                post("/api/v1/claims")
                                        .with(user(subject, "claims:write"))
                                        .header("Idempotency-Key", key)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andReturn()
                        .getResponse();
        assertEquals(201, response.getStatus(), response.getContentAsString());
        assertNotNull(response.getHeader("ETag"));
        return json.readTree(response.getContentAsString()).get("id").asText();
    }
}
