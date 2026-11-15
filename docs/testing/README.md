# Testing

## Backend

From `backend`, run `mvnw.cmd verify` on Windows or `./mvnw verify` on Linux/macOS.
Default tests use isolated H2 and simulated JWT identities. The `sqlserver-it`
profile creates uniquely named disposable databases to test the published
V1-to-V5 migration path, persistence, ownership, concurrency and audit rollback.
See [SQL Server test setup](../../backend/scripts/README.md).

## AI service

From `ai-service`, run `python -m unittest discover -s tests -v`. Tests cover
score/explanation consistency, findings, input rejection, HTTP framing and the
service key. Install `requirements-dev.txt` for Black formatting checks.
`python train.py` should reproduce the committed model artifact byte-for-byte.
Synthetic accuracy measures fit to the generator, not actual payer performance.

## Frontend and full flow

From `frontend`, run `npm ci` and `npm run verify`. Package the backend, install
Chromium with `npx playwright install chromium`, then run `npm run test:e2e`.
The browser suite launches its own backend, AI service and Vite server. It tests
claim entry, assessment, editing, submission and review without using the local
SQL Server database. Java 17, Node 24 and Python 3.12+ must be available.

CI workflows cover the backend/SQL Server, AI reproducibility and frontend/browser
suites. Generated reports, virtual environments and dependency directories are
ignored. Real identity-provider and deployment acceptance are separate checks.
