# ClaimIQ — Claim Management backend
## API
Base path: `/api/v1/claims`.
- `POST /`: create a DRAFT claim (201, Location header)
- `GET /{id}`: get a claim (200 or 404)
- `GET /?page=0&size=20`: paginated list (200; size max 100)
- `PUT /{id}`: update a DRAFT claim (200 or 409)
- `POST /{id}/submit`: DRAFT -> SUBMITTED (200 or 409)
- `PATCH /{id}/status`: SUBMITTED -> UNDER_REVIEW -> APPROVED/REJECTED (200 or 409)
Request example:
```json
{"patientReference":"PAT-DEMO-001","providerReference":"PROV-DEMO-001","amount":125.50,"currency":"USD","serviceDate":"2026-11-01"}
```
Status update example: `{"status":"UNDER_REVIEW"}`.
All examples use synthetic references. Do not send real patient data.
## Windows local setup
1. Create a SQL Server database named `ClaimIQ` and a least-privilege SQL login `claimiq_app` with appropriate schema migration privileges in development. Enable SQL authentication and TCP/IP if using this configuration.
2. In PowerShell:
```powershell
$env:SPRING_PROFILES_ACTIVE="local"
$env:CLAIMIQ_DB_USERNAME="claimiq_app"
$env:CLAIMIQ_DB_PASSWORD="<your-local-password>"
# Optionally set CLAIMIQ_DB_URL to override the local default
cd backend
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```
3. Flyway applies V1 at startup; Hibernate validates the schema. The default configuration requires explicit database environment variables; the local profile defaults only the URL.
4. Windows integrated authentication requires a different JDBC URL and the Microsoft JDBC native authentication library, and is not assumed here.
## Security and limitations
Authentication and role-based authorization are NOT implemented. Do not expose this API publicly or use real PHI. Production requires TLS certificate verification, secret management, access controls, audit event history, privacy review, and structured request correlation. Avoid request-body logging. Flyway migration has not been verified against an actual SQL Server instance.
## Validation
Run Maven tests locally with Java 17. SQL Server integration and end-to-end tests remain unverified until executed against an isolated instance.
