# ClaimIQ frontend

React and TypeScript workspace for claim entry, review and advisory risk
assessment. The side menu separates claims, workflow guidance and session details.
Draft creation/editing opens a dialog with directly accessible sections; saved
claim details have their own URL. List pagination survives navigation.

## Requirements and startup

Use Node 24 LTS with npm, Java 17, and Python 3.12+. From the repository root:

```powershell
.\frontend\dev.ps1 install
.\dev.cmd start -Database Memory
```

The root command starts AI, backend and frontend, waits for health and prints
the Writer and Reviewer tokens. Open http://localhost:5173 and connect with the
writer token. Use synthetic references and notes only. `dev.cmd stop` and
`dev.cmd restart` manage all three processes; `status` shows the current state.

For durable SQL Server storage, omit `-Database Memory` after completing
[backend setup](../backend/scripts/README.md). The memory demo resets on restart.
Demo tokens expire after one hour and rotate on backend restart. Tokens and
unsaved forms stay in browser memory; refreshing requires reconnecting.

If PowerShell blocks scripts, use a process-only invocation, for example
`powershell -NoProfile -ExecutionPolicy Bypass -File .\frontend\dev.ps1 dev`.
The standard `npm ci`, `npm run dev`, and other package scripts also work from
`frontend`. The helper can bootstrap portable npm when only Node is installed.

## Development commands

Run these from `frontend`:

| Command                           | Purpose                                                  |
| --------------------------------- | -------------------------------------------------------- |
| `npm ci`                          | Install the committed dependency versions                |
| `npm run dev`                     | Start Vite on port 5173                                  |
| `npm run verify`                  | Formatting, lint, tests, type checking and build         |
| `npm run format`                  | Format source and configuration                          |
| `npm run test`                    | Unit and component tests                                 |
| `npm run build`                   | Produce static assets in `dist`                          |
| `npx playwright install chromium` | Install the browser test runtime                         |
| `npm run test:e2e`                | Exercise the real backend and Python service in Chromium |

Browser tests require a packaged backend JAR (`backend/dev.ps1 package`) and
Python on PATH. They start isolated H2, Python and Vite processes on ports
8091, 8092 and 5174. Test signing credentials are fixed and test-only. Browser
results are written to ignored `test-results`. CI runs the same checks.

Background start/stop, port selection, backend URL selection and token retrieval
are documented in [server commands](../scripts/README.md). Root commands manage
all three services; component scripts remain available for individual development.

## Claim workflow

Create a draft with insurance, diagnoses and billed services. The form calculates
the total in integer cents; the backend verifies it independently. Optional
clinical context is limited to 2,000 characters. Read the full
[field contract](../docs/api/README.md) for limits and legacy claims.

Select **Assess denial risk** on a saved claim to see the synthetic model's
score, findings, evidence and suggested corrections. Save a correction and assess
again to compare manually. Results are invalidated when the loaded claim version
changes. Assessments are advisory, not persisted history or verified payer
responses. Read [model assumptions and limits](../ai-service/README.md).

Submission locks a draft. Reconnect with the reviewer token to start review and
approve/reject. The backend enforces scopes, ownership and transitions. Unsaved
dialogs require confirmation on navigation or closing; changing a tab never saves.

## Integration and deployment

Requests use same-origin `/api` routes. Vite proxies them to `BACKEND_URL`,
defaulting to `http://127.0.0.1:8081`; see `.env.example`. Production hosting must
serve `index.html` for client routes and proxy `/api` to Spring Boot. Vite's proxy
is development-only; `npm run preview` does not provide an API proxy. Use TLS and
a production identity provider for deployment.

Features live under `src/features`; the typed `ClaimsGateway` separates UI from
the HTTP adapter. Writes preserve ETags and never silently retry stale updates.
The server is the validation and authorization authority. Monetary entry is
limited to exact JavaScript cent arithmetic; extending large-value support
requires a decimal-string API.

Fonts are bundled locally. The visual palette references Legacy Health LLC's
public branding, without implying affiliation. Attachments, user administration,
audit-history browsing, real payer verification and production login are outside
this implementation.
