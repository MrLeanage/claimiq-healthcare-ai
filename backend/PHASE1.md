# ClaimIQ — Claim Management backend

## Package structure

The claims feature stays together under `com.claimiq.backend.claims`:

```text
claims/
├── api/         REST controller and HTTP exception handler
├── service/     ClaimService use-case contract
│   └── impl/    DefaultClaimService orchestration and transactions
├── domain/      JPA entity, claim status, and lifecycle rules
├── repository/  Spring Data JPA repository
├── dto/         Request and response records
├── mapper/      Entity/DTO mapping
└── exception/   Claim-specific exceptions
```

Tests mirror the production package structure. `BackendApplication` remains in
`com.claimiq`, above every claims package, so Spring component, entity, and
repository scanning includes the entire feature without custom scan settings.

`Claim` protects its lifecycle: `submit()` records the submission timestamp and
`transitionTo()` permits only the documented review decisions. There are no public
status or submission-time setters. Detail setters reject edits after submission,
including calls made outside the service. The service coordinates loading,
mapping, and persistence; the domain owns the transition rules.

Controllers depend on the `ClaimService` interface. Spring injects its single
`DefaultClaimService` implementation, where transaction annotations and
repository dependencies reside. Implementation tests mirror `service.impl`.

The service also depends on `ClaimNumberGenerator`, so numbering policy can be
replaced without modifying claim orchestration. `UuidClaimNumberGenerator`
preserves the existing `CLM-` plus 20 uppercase hexadecimal characters format.
HTTP errors use the immutable `ApiError` record with the same JSON fields as
before: `timestamp`, `status`, `code`, and `message`.

See [architecture decisions](../docs/architecture/README.md) for the OOP/SOLID
review scope, tradeoffs, and automated safeguards.

## API

Base path: `/api/v1/claims`.

| Method | Path | Action | Response |
| --- | --- | --- | --- |
| `POST` | `/` | Create a DRAFT claim | 201 with a Location header |
| `GET` | `/{id}` | Get a claim | 200 or 404 |
| `GET` | `/?page=0&size=20` | List claims with pagination; maximum size is 100 | 200 |
| `PUT` | `/{id}` | Update a DRAFT claim | 200 or 409 |
| `POST` | `/{id}/submit` | Move DRAFT to SUBMITTED | 200 or 409 |
| `PATCH` | `/{id}/status` | Move SUBMITTED to UNDER_REVIEW, then APPROVED or REJECTED | 200 or 409 |

### Claim status workflow

```text
DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED
                                ↘ REJECTED
```

### Create request

```json
{
  "patientReference": "PAT-DEMO-001",
  "providerReference": "PROV-DEMO-001",
  "amount": 125.50,
  "currency": "USD",
  "serviceDate": "2026-11-01"
}
```

### Status update request

```json
{
  "status": "UNDER_REVIEW"
}
```

All examples use synthetic references. Do not send real patient data.

## Windows local setup

1. Create a SQL Server database named `ClaimIQ` and a least-privilege SQL login
   `claimiq_app` with appropriate schema migration privileges in development.
   Enable SQL authentication and TCP/IP if using this configuration.

2. From the repository root, run the following in PowerShell:

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "local"
   $env:CLAIMIQ_DB_USERNAME = "claimiq_app"
   $env:CLAIMIQ_DB_PASSWORD = "<your-local-password>"
   # Optionally set CLAIMIQ_DB_URL to override the local default.
   cd backend
   .\mvnw.cmd clean verify
   .\mvnw.cmd spring-boot:run
   ```

3. Flyway applies V1 at startup; Hibernate validates the schema. The default
   configuration requires explicit database environment variables; the local
   profile defaults only the URL.

4. Windows integrated authentication requires a different JDBC URL and the
   Microsoft JDBC native authentication library, and is not assumed here.

## Security and limitations

Authentication and role-based authorization are **not implemented**. Do not expose
this API publicly or use real PHI.

Production requires:

- TLS certificate verification and secret management.
- Access controls and audit event history.
- Privacy review and structured request correlation.

Avoid request-body logging. Flyway migration has not been verified against an
actual SQL Server instance.

## Validation

Run Maven tests locally with Java 17. SQL Server integration and end-to-end tests
remain unverified until executed against an isolated instance.
