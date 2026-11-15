package com.claimiq.backend.assessment;

import com.claimiq.backend.claims.TestClaimData;
import com.claimiq.backend.claims.domain.ClaimStatus;
import com.claimiq.backend.claims.dto.ClaimResponse;
import com.claimiq.backend.claims.exception.ClaimVersionConflictException;
import com.claimiq.backend.claims.service.ClaimService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssessmentServiceTest {
    private final ClaimService claims = mock(ClaimService.class);
    private final UUID id = UUID.randomUUID();

    private ClaimResponse claim(long version) {
        return new ClaimResponse(id, "SECRET-CLAIM", "SECRET-PATIENT", "SECRET-PROVIDER",
                BigDecimal.TEN, "USD", LocalDate.now(), ClaimStatus.DRAFT,
                null, null, null, version, TestClaimData.dto(BigDecimal.TEN));
    }

    @Test
    void accessAndVersionChecksHappenBeforeRemoteRequest() {
        var service = new AssessmentService(claims, "http://127.0.0.1:1", "");
        when(claims.get(id)).thenThrow(new AccessDeniedException("Denied"));
        assertThrows(AccessDeniedException.class, () -> service.assess(id, 0));
        doReturn(claim(1)).when(claims).get(id);
        assertThrows(ClaimVersionConflictException.class, () -> service.assess(id, 0));
    }

    @Test
    void unavailableServiceNeverReturnsFabricatedLowRisk() {
        when(claims.get(id)).thenReturn(claim(0));
        var service = new AssessmentService(claims, "http://127.0.0.1:1", "");
        var error = assertThrows(ResponseStatusException.class, () -> service.assess(id, 0));
        assertEquals(503, error.getStatusCode().value());
    }

    @Test
    void mapsResponseMinimizesPayloadAndRejectsConcurrentEdits() throws Exception {
        var received = new java.util.concurrent.atomic.AtomicReference<String>();
        var key = new java.util.concurrent.atomic.AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/assess", exchange -> {
            received.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            key.set(exchange.getRequestHeaders().getFirst("X-AI-Key"));
            byte[] body = """
                {"modelVersion":"synthetic-logistic-v1","trainingData":"SYNTHETIC",
                 "assessedAt":"2026-11-15T00:00:00Z","score":50,"riskLevel":"MEDIUM",
                 "interceptLogOdds":-3,"findings":[],"limitations":["Demo"]}
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            var service = new AssessmentService(claims, "http://127.0.0.1:" + server.getAddress().getPort(), "test-key");
            when(claims.get(id)).thenReturn(claim(0));
            var response = service.assess(id, 0);
            assertEquals(50, response.assessment().score());
            assertEquals(0, response.claimVersion());
            assertEquals("test-key", key.get());
            assertFalse(received.get().contains("SECRET"));
            assertFalse(received.get().contains("MEM-DEMO"));
            assertFalse(received.get().contains("Synthetic clinical context"));
            assertTrue(received.get().contains("coverageStatus"));
            when(claims.get(id)).thenReturn(claim(0), claim(1));
            assertThrows(ClaimVersionConflictException.class, () -> service.assess(id, 0));
        } finally {
            server.stop(0);
        }
    }
}
