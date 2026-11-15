package com.claimiq.backend.claims.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "CLAIMIQ_DEMO_JWT_SECRET=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class ClaimOpenApiTest {
    @Autowired MockMvc mvc;

    @Test
    void documentsBearerAuthenticationAndRequiredVersionHeaders() throws Exception {
        var result = mvc.perform(get("/api-docs")).andReturn().getResponse();
        assertEquals(200, result.getStatus());
        var spec = JsonMapper.builder().build().readTree(result.getContentAsString());
        assertEquals("bearer", spec.at("/components/securitySchemes/bearerAuth/scheme").asText());
        for (String operation :
                new String[] {
                    "/paths/~1api~1v1~1claims~1{id}/put",
                    "/paths/~1api~1v1~1claims~1{id}~1submit/post",
                    "/paths/~1api~1v1~1claims~1{id}~1status/patch"
                }) {
            boolean documented = false;
            for (var parameter : spec.at(operation + "/parameters")) {
                if (parameter.get("name").asText().equals("If-Match")) {
                    assertTrue(parameter.get("required").asBoolean());
                    documented = true;
                }
            }
            assertTrue(documented, operation);
        }
        assertEquals(
                200,
                mvc.perform(get("/swagger-ui/index.html")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(get("/api/v1/claims")).andReturn().getResponse().getStatus());
        assertEquals(
                401,
                mvc.perform(get("/api/v1/claims").header("Authorization", "Bearer invalid"))
                        .andReturn()
                        .getResponse()
                        .getStatus());
    }

    @Test
    void signedDemoTokensSupportTheClaimWorkflow() throws Exception {
        String writer = token("demo-writer", "claims:read claims:write", 3600, new byte[32]);
        String reviewer = token("demo-reviewer", "claims:read claims:review", 3600, new byte[32]);
        var created =
                mvc.perform(
                                post("/api/v1/claims")
                                        .header("Authorization", "Bearer " + writer)
                                        .contentType("application/json")
                                        .content(
                                                """
{"patientReference":"PAT-DEMO","providerReference":"PROV-DEMO",
 "amount":125.50,"currency":"USD","serviceDate":"2026-01-01","context":{"insurance":{"payerName":"Demo payer","planReference":"PLAN-DEMO","memberReference":"MEM-DEMO","coverageStatus":"UNKNOWN","coverageVerifiedOn":null,"authorizationStatus":"NOT_REQUIRED","authorizationReference":null},"diagnosisCodes":["Z00.00"],"serviceLines":[{"codeSystem":"LOCAL","procedureCode":"DEMO-001","description":"Synthetic consultation","units":1,"unitPrice":125.5}],"clinicalSummary":"Synthetic clinical context"}}
"""))
                        .andReturn()
                        .getResponse();
        assertEquals(201, created.getStatus());
        String path = created.getHeader("Location");
        var submitted =
                mvc.perform(
                                post(path + "/submit")
                                        .header("Authorization", "Bearer " + writer)
                                        .header("If-Match", created.getHeader("ETag")))
                        .andReturn()
                        .getResponse();
        assertEquals(200, submitted.getStatus());
        assertEquals(
                403,
                mvc.perform(
                                patch(path + "/status")
                                        .header("Authorization", "Bearer " + writer)
                                        .header("If-Match", submitted.getHeader("ETag"))
                                        .contentType("application/json")
                                        .content("{\"status\":\"UNDER_REVIEW\"}"))
                        .andReturn()
                        .getResponse()
                        .getStatus());
        assertEquals(
                200,
                mvc.perform(
                                patch(path + "/status")
                                        .header("Authorization", "Bearer " + reviewer)
                                        .header("If-Match", submitted.getHeader("ETag"))
                                        .contentType("application/json")
                                        .content("{\"status\":\"UNDER_REVIEW\"}"))
                        .andReturn()
                        .getResponse()
                        .getStatus());
    }

    @Test
    void expiredAndWronglySignedDemoTokensAreRejected() throws Exception {
        byte[] otherKey = new byte[32];
        otherKey[0] = 1;
        for (String invalid :
                new String[] {
                    token("writer", "claims:read", -300, new byte[32]),
                    token("writer", "claims:read", 3600, otherKey)
                }) {
            assertEquals(
                    401,
                    mvc.perform(get("/api/v1/claims").header("Authorization", "Bearer " + invalid))
                            .andReturn()
                            .getResponse()
                            .getStatus());
        }
    }

    private String token(String subject, String scope, long lifetime, byte[] key) throws Exception {
        var now = java.time.Instant.now();
        var claims =
                new com.nimbusds.jwt.JWTClaimsSet.Builder()
                        .subject(subject)
                        .issuer("claimiq-demo")
                        .audience("claimiq-api")
                        .issueTime(java.util.Date.from(now.minusSeconds(600)))
                        .expirationTime(java.util.Date.from(now.plusSeconds(lifetime)))
                        .claim("scope", scope)
                        .build();
        var jwt =
                new com.nimbusds.jwt.SignedJWT(
                        new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256),
                        claims);
        jwt.sign(new com.nimbusds.jose.crypto.MACSigner(key));
        return jwt.serialize();
    }
}
