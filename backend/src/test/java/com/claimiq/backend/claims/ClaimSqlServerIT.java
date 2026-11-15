package com.claimiq.backend.claims;

import static org.junit.jupiter.api.Assertions.*;

import com.claimiq.backend.claims.domain.Claim;
import com.claimiq.backend.claims.dto.ClaimRequest;
import com.claimiq.backend.claims.exception.ClaimVersionConflictException;
import com.claimiq.backend.claims.repository.ClaimAuditRepository;
import com.claimiq.backend.claims.service.ClaimService;

import jakarta.persistence.EntityManagerFactory;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Runs only with -Psqlserver-it, always against a newly created disposable database. */
@SpringBootTest
@org.springframework.test.annotation.DirtiesContext(
        classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class ClaimSqlServerIT {
    private static final String DATABASE =
            "ClaimIQ_it_" + UUID.randomUUID().toString().replace("-", "");
    private static final UUID LEGACY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static String adminUrl;
    private static String databaseUrl;
    private static String username;
    private static String password;
    private static boolean created;
    @Autowired ClaimService service;
    @Autowired ClaimAuditRepository audit;
    @Autowired EntityManagerFactory entityManagers;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired com.claimiq.backend.claims.repository.ClaimRepository claims;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) throws Exception {
        adminUrl = System.getenv("CLAIMIQ_IT_ADMIN_URL");
        if (adminUrl == null || !adminUrl.startsWith("jdbc:sqlserver://")) {
            throw new IllegalStateException(
                    "Set CLAIMIQ_IT_ADMIN_URL for a SQL Server test account with CREATE DATABASE"
                            + " permission");
        }
        username = System.getenv().getOrDefault("CLAIMIQ_IT_USERNAME", "");
        password = System.getenv().getOrDefault("CLAIMIQ_IT_PASSWORD", "");
        databaseUrl =
                adminUrl.replaceAll("(?i);databaseName=[^;]*", "") + ";databaseName=" + DATABASE;
        try (var connection = DriverManager.getConnection(adminUrl, username, password);
                var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE [" + DATABASE + "]");
            created = true;
        }
        // Start from the original published V1, with synthetic data, then let Boot
        // apply forward migrations and validate the actual SQL Server schema.
        Flyway.configure().dataSource(databaseUrl, username, password).target("1").load().migrate();
        try (var connection = DriverManager.getConnection(databaseUrl, username, password);
                var statement = connection.createStatement()) {
            statement.executeUpdate(
                    """
INSERT INTO claims (id, claim_number, patient_reference, provider_reference, amount,
    currency, service_date, status, created_at, updated_at, version)
VALUES ('11111111-1111-1111-1111-111111111111', 'CLM-LEGACY', 'PAT-DEMO',
    'PROV-DEMO', 125.00, 'USD', '2026-01-01', 'DRAFT',
    '2026-01-01T12:34:56.123456', '2026-01-01T12:34:56.123456', 0)
""");
        }
        properties.add("spring.datasource.url", () -> databaseUrl);
        properties.add("spring.datasource.username", () -> username);
        properties.add("spring.datasource.password", () -> password);
        properties.add(
                "spring.datasource.driver-class-name",
                () -> "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("spring.flyway.enabled", () -> "true");
    }

    @BeforeEach
    void authenticate() {
        var token =
                Jwt.withTokenValue("test-only")
                        .header("alg", "RS256")
                        .subject("sql-test-owner")
                        .build();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(
                                token, List.of(new SimpleGrantedAuthority("SCOPE_claims:write"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void forwardMigrationPreservesUtcDataAndMatchesHibernate() throws Exception {
        try (var connection = DriverManager.getConnection(databaseUrl, username, password);
                var statement = connection.createStatement();
                var rows =
                        statement.executeQuery(
                                "SELECT created_at, owner_subject FROM claims WHERE id='"
                                        + LEGACY_ID
                                        + "'")) {
            assertTrue(rows.next());
            assertEquals(
                    Instant.parse("2026-01-01T12:34:56.123456Z"),
                    rows.getObject(1, OffsetDateTime.class).toInstant());
            assertNull(rows.getString(2));
        }
    }

    @Test
    void writesReturnCommittedMetadataAndRejectStaleFormsWithoutAudit() {
        var original = service.create(request("125.00"));
        var updated = service.update(original.id(), request("150.00"), original.version());
        var readBack = service.get(original.id());
        assertEquals(original.version() + 1, updated.version());
        assertEquals(readBack.version(), updated.version());
        // SQL Server's precision is 100 ns; compare at its actual persisted precision.
        assertEquals(readBack.updatedAt(), updated.updatedAt());
        assertThrows(
                ClaimVersionConflictException.class,
                () -> service.update(original.id(), request("125.00"), original.version()));
        assertEquals(new BigDecimal("150.00"), service.get(original.id()).amount());
        assertEquals(2, audit.findAllByClaimIdOrderByOccurredAtAsc(original.id()).size());
    }

    @Test
    void overlappingTransactionsCannotBothCommit() {
        var original = service.create(request("125.00"));
        try (var first = entityManagers.createEntityManager();
                var second = entityManagers.createEntityManager()) {
            first.getTransaction().begin();
            second.getTransaction().begin();
            Claim a = first.find(Claim.class, original.id());
            Claim b = second.find(Claim.class, original.id());
            a.setAmount(new BigDecimal("150.00"));
            first.getTransaction().commit();
            b.setAmount(new BigDecimal("175.00"));
            assertThrows(jakarta.persistence.OptimisticLockException.class, second::flush);
            second.getTransaction().rollback();
        }
        assertEquals(new BigDecimal("150.00"), service.get(original.id()).amount());
    }

    private ClaimRequest request(String amount) {
        return new ClaimRequest(
                "PAT-DEMO", "PROV-DEMO", new BigDecimal(amount), "USD", LocalDate.of(2026, 1, 1));
    }

    @Test
    void ownershipUsesExactCaseSensitiveUnicodeSubjects() {
        String suffix = UUID.randomUUID().toString();
        var token =
                Jwt.withTokenValue("test-only")
                        .header("alg", "RS256")
                        .subject("CaseOwner-" + suffix)
                        .build();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(
                                token, List.of(new SimpleGrantedAuthority("SCOPE_claims:write"))));
        var owned = service.create(request("125.00"));
        token =
                Jwt.withTokenValue("test-only")
                        .header("alg", "RS256")
                        .subject("caseowner-" + suffix)
                        .build();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(
                                token, List.of(new SimpleGrantedAuthority("SCOPE_claims:write"))));
        assertEquals(
                0,
                service.list(org.springframework.data.domain.PageRequest.of(0, 20))
                        .getTotalElements());
        assertThrows(
                com.claimiq.backend.claims.exception.ClaimNotFoundException.class,
                () -> service.get(owned.id()));
        token =
                Jwt.withTokenValue("test-only")
                        .header("alg", "RS256")
                        .subject("用户-" + suffix)
                        .build();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(
                                token, List.of(new SimpleGrantedAuthority("SCOPE_claims:write"))));
        var unicodeOwned = service.create(request("125.00"));
        assertEquals(
                "用户-" + suffix, claims.findById(unicodeOwned.id()).orElseThrow().getOwnerSubject());
        assertEquals(unicodeOwned.id(), service.get(unicodeOwned.id()).id());
    }

    @Test
    void auditFailureRollsBackTheClaimInTheSameTransaction() {
        long countBefore = claims.count();
        var createdId = new java.util.concurrent.atomic.AtomicReference<UUID>();
        var transaction =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        assertThrows(
                RuntimeException.class,
                () ->
                        transaction.execute(
                                status -> {
                                    var response = service.create(request("125.00"));
                                    createdId.set(response.id());
                                    audit.save(
                                            new com.claimiq.backend.claims.domain.ClaimAuditEvent(
                                                    claims.findById(response.id()).orElseThrow(),
                                                    null,
                                                    "INVALID_AUDIT",
                                                    null));
                                    return null;
                                }));
        assertNotNull(createdId.get());
        assertEquals(countBefore, claims.count());
        assertTrue(audit.findAllByClaimIdOrderByOccurredAtAsc(createdId.get()).isEmpty());
    }

    @AfterAll
    static void dropDisposableDatabase() throws Exception {
        if (created) {
            if (!DATABASE.matches("ClaimIQ_it_[0-9a-f]{32}"))
                throw new IllegalStateException("Unsafe cleanup target");
            try (var connection = DriverManager.getConnection(adminUrl, username, password);
                    var statement = connection.createStatement()) {
                statement.execute(
                        "ALTER DATABASE ["
                                + DATABASE
                                + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                statement.execute("DROP DATABASE [" + DATABASE + "]");
            }
        }
    }
}
