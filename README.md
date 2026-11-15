# ClaimIQ

A healthcare claims review application. Claims staff prepare drafts, submit them
for review, and record decisions through a versioned, authenticated API.
Development and demonstrations use synthetic data.

## Backend

Java 17, Spring Boot, SQL Server, Flyway and JWT bearer authentication.

- [Backend setup and API guide](backend/README.md)
- [Claim context contract](docs/api/README.md)
- [Development server management](scripts/README.md)

From `backend`, run `mvnw.cmd test` on Windows or `./mvnw test` on Linux/macOS.
See the backend guide for SQL Server configuration and the disposable H2 demo.

The local demo uses short-lived tokens. Production identity-provider integration,
deployment infrastructure, and regulatory acceptance require separate validation.

## Assessment service

The Python service returns an advisory score, contributing factors and suggested
review actions through Spring Boot. It uses logistic regression trained on
generated outcomes; scores are not validated payer denial probabilities.

From `ai-service`, run `python -m app.server --port 8090` in a separate terminal.
Check `http://127.0.0.1:8090/health`. The backend uses that address by default.
See [AI service setup, training and limitations](ai-service/README.md).

## Web workspace

The [React frontend](frontend/README.md) supports claim entry, correction,
assessment and human review. From the repository root, install frontend
dependencies once with `.\frontend\dev.ps1 install`, then use
`.\dev.ps1 start`, `.\dev.ps1 status`, `.\dev.ps1 tokens` and `.\dev.ps1 stop`
to manage Spring Boot and Vite. Start Python separately as described above.

- [Testing](docs/testing/README.md)
- [Security boundaries](docs/security/README.md)
- [Operations](docs/operations/README.md)

## Run all services with Docker

See [Docker setup](infrastructure/docker/README.md). Generate unique local demo
credentials, then run `docker compose up -d --build --wait` from this folder.
This starts the web interface, backend and model with an isolated persistent
SQL Server database. Open http://localhost:5173. Native scripts remain available
for development; Docker does not use the existing Windows database.

Azure/Kubernetes deployment, a Jupyter evaluation notebook and persisted
assessment history remain planned work.
