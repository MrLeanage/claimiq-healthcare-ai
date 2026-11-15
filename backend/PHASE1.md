# ClaimIQ — Claim Management backend

## Structure

The claims feature lives in `com.claimiq.backend.claims`, with `api`, `service`,
`service.impl`, `domain`, `repository`, `dto`, `mapper`, `exception`, and `security`
packages. `ClaimService`, `ClaimNumberGenerator`, and `ClaimAccessPolicy` define
use-case, numbering, and access-policy boundaries. The domain owns validation and
lifecycle rules. The service owns transactions, persistence, and audit recording.

## API and concurrency contract

Base path: `/api/v1/claims`. Use synthetic references only during development.

| Method | Path | Permission | Result |
| --- | --- | --- | --- |
| POST | `/` | `claims:write` | Create owned DRAFT; 201, Location, ETag |
| GET | `/{id}` | Owner with read/write scope, or reviewer | 200 with ETag; otherwise 404 |
| GET | `/?page=0&size=20` | Read/write scope or reviewer | Owner's claims; reviewers can see all; size 1–100 |
| PUT | `/{id}` | Owner with `claims:write` | Update DRAFT; requires If-Match |
| POST | `/{id}/submit` | Owner with `claims:write` | Submit complete DRAFT; requires If-Match |
| PATCH | `/{id}/status` | `claims:review` | Review transition; requires If-Match |

Read the `ETag` from create/get/write responses and send it unchanged in `If-Match`
on every update, submission, and review transition. For example:

```http
PATCH /api/v1/claims/00000000-0000-0000-0000-000000000000/status
Authorization: Bearer <token>
If-Match: "1"
Content-Type: application/json

{"status":"UNDER_REVIEW"}
```

Missing preconditions return 428; stale versions, including concurrent optimistic
locking failures, return 412. Reload before retrying; never silently retry a stale
write. Malformed preconditions return 400. Invalid lifecycle transitions return
409. Successful writes flush before mapping their current version and timestamp.
List ordering uses `createdAt` and `id` for a deterministic tie-breaker.

```text
DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED
                                ↘ REJECTED
```

Create/update request:

```json
{
  "patientReference": "PAT-DEMO-001",
  "providerReference": "PROV-DEMO-001",
  "amount": 125.50,
  "currency": "USD",
  "serviceDate": "2026-01-01"
}
```

The domain rejects blank/oversized references, nonpositive or oversized amounts,
more than two fractional digits, invalid currency codes, and future service dates.
Incomplete drafts cannot be submitted. Detail updates validate atomically before
changing fields. Lifecycle timestamps use UTC microsecond precision to round-trip
through SQL Server without returning extra, unpersisted nanoseconds.

## Authentication and ownership

The API accepts JWT bearer tokens only. Configure:

- `CLAIMIQ_JWT_ISSUER`: trusted identity provider's issuer URL.
- `CLAIMIQ_JWT_AUDIENCE`: the audience identifying this API.

The decoder verifies the issuer, audience, signature, and normal JWT timestamps.
The provider must grant `claims:read`, `claims:write`, or `claims:review` through
the standard `scope`/`scp` claim. Tokens need a stable, nonblank `sub` of at most
255 characters. Users cannot supply or change a claim's owner through the API.

If issuer or audience is unset, the application starts but rejects all bearer
tokens. There are no hardcoded development passwords or anonymous claim routes.
Only `/actuator/health` is public. Stateless bearer authentication does not use
browser session cookies. Configure TLS at deployment; do not use real data until
the identity provider and permissions are tested end to end.

Reviewers can read and review all claims in this single-tenant prototype. A user
with both write and review scopes can review their own claim; stricter separation
of duties or tenancy requires an explicit business-policy decision.

Create, update, submit, and transition operations append the actor, claim ID,
previous/new status, committed entity version, and UTC event time in the same
database transaction. Audit failure rolls back the claim change. No claim body,
patient reference, or token is copied into the audit row. Restrict database-level
audit-table permissions in production; application append-only behavior is not
a tamper-proof external audit system.

## Database configuration and migrations

Production configuration requires `CLAIMIQ_DB_URL`, `CLAIMIQ_DB_USERNAME`, and
`CLAIMIQ_DB_PASSWORD`. The published local profile supplies a local SQL Server URL
and development username; set the password outside version control. Windows
authentication additionally requires the matching Microsoft JDBC authentication
DLL and an integrated-security URL. Existing uncommitted local overrides are not
part of the PR.

- V1 remains unchanged to preserve its published Flyway checksum.
- V2 converts legacy UTC `datetime2(6)` fields to `datetimeoffset(7)` and rebuilds
  the affected index. Review the UTC assumption before upgrading imported data.
- V3 adds ownership and transactional audit storage.

Historical rows have no owner. Only reviewers can read them; users cannot edit or
submit them until an administrator explicitly reconciles ownership. Never assign
historical claims to arbitrary new users automatically.

**Existing local database:** a database initialized from an edited V1 has a
different checksum from the published V1. Preserve its matching migration file
until a deliberate reconciliation plan is approved. Do not run `flyway repair`,
delete schema history, or overwrite that file merely to bypass validation. These
fixes do not modify or repair the existing local ClaimIQ database.

## Verification

From `backend`, with Java 17:

```powershell
.\mvnw.cmd clean verify
```

The default suite includes domain/service tests, Spring context loading, and
authenticated HTTP/ownership tests using an isolated H2 test database and simulated
JWT identities. It does not connect to the workstation database.

For the SQL Server integration gate, configure a test account with CREATE DATABASE
permission on a dedicated development/test instance:

```powershell
$env:CLAIMIQ_IT_ADMIN_URL = "jdbc:sqlserver://localhost:1433;databaseName=master;encrypt=true;trustServerCertificate=true"
$env:CLAIMIQ_IT_USERNAME = "<test-admin>"
$env:CLAIMIQ_IT_PASSWORD = "<test-password>"
.\mvnw.cmd -Psqlserver-it clean verify
```

Integrated authentication is also supported by the test URL and matching DLL.
The integration test creates a uniquely named `ClaimIQ_it_<random>` database,
applies original V1, inserts synthetic data, applies V2/V3, validates Hibernate,
and checks metadata, stale writes, concurrent transactions, and audit rollback.
It drops only that generated database. Do not point the test administrator at a
production server. CI runs the same profile with a disposable SQL Server service.

Tests use simulated JWT users; real identity-provider login remains a deployment
configuration and acceptance-test step.

See [architecture decisions](../docs/architecture/README.md) for OOP/SOLID boundaries.
