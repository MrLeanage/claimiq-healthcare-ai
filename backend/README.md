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
