package com.claimiq.backend.assessment;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.claimiq.backend.claims.TestClaimData;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

class HttpAssessmentClientTest {
    private static final String VALID =
            """
            {"modelVersion":"synthetic-logistic-v1","trainingData":"SYNTHETIC",
             "assessedAt":"2026-11-15T00:00:00Z","score":50,"riskLevel":"MEDIUM",
             "interceptLogOdds":-3,"findings":[],"limitations":["Demo"]}
            """;

    @Test
    void rejectsMalformedResultsInsteadOfReturningRisk() throws Exception {
        List<String> invalid =
                List.of(
                        VALID.replace("\"findings\":[]", "\"findings\":[null]"),
                        VALID.replace("\"findings\":[]", "\"findings\":[{}]"),
                        VALID.replace("\"limitations\":[\"Demo\"]", "\"limitations\":[null]"),
                        VALID.replace("\"score\":50,", ""),
                        VALID.replace("\"interceptLogOdds\":-3,", ""),
                        VALID.replace("\"score\":50", "\"score\":101"),
                        VALID.replace("synthetic-logistic-v1", " "),
                        VALID.replace("2026-11-15T00:00:00Z", "invalid-date"),
                        VALID.replace("SYNTHETIC", "REAL"));
        for (String body : invalid) {
            withResponse(
                    200,
                    body,
                    client -> {
                        var error =
                                assertThrows(
                                        ResponseStatusException.class,
                                        () -> client.assess(TestClaimData.dto(BigDecimal.TEN)));
                        assertEquals(503, error.getStatusCode().value());
                        assertTrue(error.getReason().contains("reference"));
                    });
        }
    }

    @Test
    void logsAuthenticationCategoryAndReferenceWithoutDependencySecrets() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(HttpAssessmentClient.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            withResponse(
                    403,
                    "SECRET-CLAIM SECRET-KEY",
                    client -> {
                        var error =
                                assertThrows(
                                        ResponseStatusException.class,
                                        () -> client.assess(TestClaimData.dto(BigDecimal.TEN)));
                        String message =
                                appender.list.get(appender.list.size() - 1).getFormattedMessage();
                        assertTrue(message.contains("category=AUTHENTICATION"));
                        String reference =
                                error.getReason().substring(error.getReason().lastIndexOf(' ') + 1);
                        assertTrue(message.contains(reference));
                        assertFalse(message.contains("SECRET"));
                        assertNull(appender.list.get(appender.list.size() - 1).getThrowableProxy());
                        assertFalse(error.getReason().contains("Start the AI"));
                    });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private void withResponse(
            int status, String body, java.util.function.Consumer<HttpAssessmentClient> test)
            throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/assess",
                exchange -> {
                    exchange.getRequestBody().readAllBytes();
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(status, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                });
        server.start();
        try {
            test.accept(
                    new HttpAssessmentClient(
                            "http://127.0.0.1:" + server.getAddress().getPort(), "SECRET-KEY"));
        } finally {
            server.stop(0);
        }
    }
}
