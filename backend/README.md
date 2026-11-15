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
