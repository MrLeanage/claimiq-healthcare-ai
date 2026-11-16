# ClaimIQ backend

Java 17 / Spring Boot claims API backed by SQL Server. Supports owned drafts,
submission and reviewer decisions, versioned updates, and transactional audit events.

- [API, security and migration guide](PHASE1.md)
- [Build and development commands](scripts/README.md)
- [Claim context request examples](../docs/api/README.md)

Run the isolated test suite with `./mvnw test` (`mvnw.cmd test` on Windows).
For a disposable demo without SQL Server, run `dev.ps1 demo -Database Memory`.
The default demo uses SQL Server and requires the matching Windows authentication
library. Demo credentials are only for local synthetic data.

## Assessment integration

`POST /api/v1/claims/{id}/assessment` requires a bearer token with claim access and
the latest `If-Match` ETag. Start the Python service separately. Configure
`CLAIMIQ_AI_BASE_URL` and optional `CLAIMIQ_AI_API_KEY` for the backend/service pair.
See [API details](../docs/api/README.md) and [model limits](../ai-service/README.md).

Assessment results are validated before leaving the inference boundary, including
model metadata, timestamp, score and every finding. Dependency failures return
503 with a reference that matches a sanitized backend warning. Warnings distinguish
transport, contract, authentication and other HTTP failures without logging claim
bodies, credentials or upstream response text.

## Creation retries and list responses

`POST /api/v1/claims` accepts an optional `Idempotency-Key` UUID. Reuse it with
the same mapped payload after an uncertain response. A successful retry returns
the existing claim's current representation, Location and ETag with status 201.
Reusing the key with different data returns 409 `IDEMPOTENCY_CONFLICT`. Keys are
scoped to the exact authenticated subject; callers without a key retain the old
creation behavior. A changed numeric scale in the mapped JSON can count as a
different payload, so clients should resend the same payload.

V6 adds a unique reservation per subject/key. The reservation, draft and CREATE
audit event commit atomically. Concurrent retries resolve through the database
constraint, not an in-memory lock. Reservations retain a request hash and claim ID,
not a second copy of the clinical payload, and currently live for the claim's
lifetime. Historical claims remain unchanged.

`GET /api/v1/claims` returns summary projections without `context`; use the
authorized detail endpoint for insurance, diagnoses, service lines and clinical
summary. The projection avoids loading the clinical collections.

## Verification and database history

Tests use a single fork with a 384 MB maximum Java heap and two visible processors.
Override `test.jvm.args` if another environment needs different limits. Mockito
uses an explicit test agent following its [instrumentation guidance](https://github.com/mockito/mockito/blob/main/mockito-core/src/main/java/org/mockito/Mockito.java),
so tests do not depend on dynamic self-attachment. This affects test JVMs only.

See [migration recovery](../docs/operations/migration-recovery.md) before working
with a database created using an earlier V1 variant. Do not rewrite an applied
migration or automatically repair an unverified checksum mismatch.
