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
