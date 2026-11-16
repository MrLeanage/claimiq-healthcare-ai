# Code walkthrough: UI, persistence and inference

Read one route at a time. Comments in the source explain responsibilities and
important values; this index supplies the order. Use your own understanding when
presenting the project, and distinguish tested behavior from planned work.

## 1. How React starts and selects a screen

1. `frontend/index.html`: the root container and entry script.
2. `frontend/src/main.tsx`: React mounting, router and draft guard.
3. `frontend/src/app/App.tsx`: local session, API client, shell and screen selection.
4. `frontend/src/features/auth/Connect.tsx`: accept a token and verify access through an API call.
5. `frontend/src/features/claims/ClaimsWorkspace.tsx`: route selection and new/edit overlay.

JSX describes elements/components. State is data React retains between renders;
setting it requests a render. Props are inputs passed from a parent component.
Components do not become separate servers. The browser executes compiled JavaScript,
and TypeScript annotations do not validate runtime JSON or authenticate a token.

## 2. Read the list, then a single claim

`ClaimsListPage` -> `useClaims` -> `HttpClaimsGateway.list` -> `request` in
`frontend/src/services/http.ts` -> Vite's development proxy -> Spring Security ->
`ClaimController.list` -> `DefaultClaimService.list` -> `ClaimRepository`.

Opening details uses `useClaim` -> `HttpClaimsGateway.get` -> the backend detail
endpoint, then `readVersioned` keeps the claim and its quoted ETag together.
List projections omit clinical context; the authorized detail fetch includes it.

Explain these TypeScript constructs in the actual HTTP helper:

- `async`/`await`: a promise-based result; awaiting network I/O does not block the browser thread.
- `Fetcher = typeof fetch`: callable contract that allows an HTTP test double.
- `init = {}`: default options; `...init` makes a shallow copy and later properties win.
- `headers` shorthand means `headers: headers`.
- `credentials: 'omit'` omits browser-managed credentials, not the explicit bearer header.
- `cache: 'no-store'` bypasses and avoids storage in the browser HTTP cache.
- `response.ok` checks 200-299; HTTP failures need explicit handling after fetch resolves.
- `unknown` requires checks before property access; a TypeScript assertion alone is not validation.
- `ApiError` extends JavaScript Error; its parameter property stores status, and `super(message)` supplies the inherited message.
- AbortSignal cancels the browser operation; it cannot guarantee cancellation of server processing already underway.

## 3. Create and update a draft

`ClaimForm`/`ClaimContextFields` -> `contextForm.buildContext` -> editor `save` in
`ClaimPages` -> `HttpClaimsGateway.create` -> HTTP POST -> `ClaimController.create`
-> `DefaultClaimCreationService` -> `DefaultClaimService` -> `ClaimMapper` -> `Claim`
-> claim/audit repositories.

Required-value checks include hidden tabs and all service lines. Format checks
still run on submit; the backend/domain revalidate independently. Amounts are
calculated using integer cents in the frontend and BigDecimal in Java.

The UI retains one UUID creation key per open form. The database's subject/key
uniqueness handles simultaneous retries. A transaction commits the reservation,
claim and CREATE audit together, or rolls everything back. Changed input with an
already used key returns 409; callers without keys retain legacy creation behavior.
The stored SHA-256 hash is a comparison aid, not encryption or authentication.

Updates use PUT and If-Match. `VersionPrecondition` checks the header shape;
`DefaultClaimService` checks the expected version and `@Version` catches concurrent
database writes. ETags are versions, not permissions. A 412 requires explicit reload.

## 4. Follow assessment across the Java/Python boundary

1. `ClaimAssessment.run` -> `HttpClaimsGateway.assess` -> shared `request`.
2. `AssessmentController.assess` parses If-Match.
3. `AssessmentService.assess` loads the claim through the access-checked claim service.
4. `HttpAssessmentClient.assess` calls `indicators` and sends POST `/assess` to Python.
5. Python `Handler.do_POST` parses JSON and calls `assessment.assess`.
6. `assessment.assess` builds ordered binary features and calls `model.probability` -> `sigmoid`.
7. Curated findings and corrections are returned as JSON.
8. Java's `AssessmentResult` constructors validate the response; the coordinator rechecks the claim version.
9. The React panel displays the result for that exact claim ID/version.

Browser DevTools shows browser-to-Java requests, not Java-to-Python traffic. Python
receives statuses, date, code systems and presence indicators, not reference IDs,
actual code identifiers, amounts or clinical text. The model does not interpret
clinical language, verify payer policy or choose approval/rejection.

Explain the constants rather than treating them as magic numbers:

- Eight features: the artifact ordering must match inference and training exactly.
- `2` and `5` seconds in the HTTP client: connection/request timeout configuration.
- `65536` bytes: the development Python endpoint's 64 KiB body cap.
- `0.30` and `0.65`: synthetic demonstration risk cutoffs on unrounded model output.
- Model intercept plus weighted features gives log-odds; sigmoid maps this to 0-1.
- Displayed score is the output multiplied by 100, rounded to one decimal.
- Finding contributions are log-odds, not percentages or proof of causation.

JSON stores learned parameters/metadata; Python is the executable inference code.
The service validates and loads the artifact at startup. Replacing it requires a
deliberate restart. Assessments are transient rather than persisted audit history.

## 5. Understand training separately

`ai-service/train.py` generates invented examples and fits logistic regression.
Seed `2718` makes the run reproducible; 4,000 rows split into 3,200 training and
800 held-out rows. Full-batch gradient descent runs 700 iterations with step size
0.5 and weight penalty strength 0.001. The intercept is unpenalized.

Held-out accuracy/Brier score evaluate imitation of the synthetic generator.
They do not establish performance on real claims. Saving drafts, receiving
assessments and approving claims do not automatically retrain the model. Jupyter
is optional for exploration and is not in the live request path.

## 6. Submit and review

`ClaimDetails` -> gateway `submit`/`transition` -> controller -> claim service ->
`Claim.submit`/`transitionTo` -> persistence and lifecycle audit.

Allowed flow: DRAFT -> SUBMITTED -> UNDER_REVIEW -> APPROVED or REJECTED.
Draft writes require ownership/write scope; review requires review scope. Reviewers
see all claims in this single-tenant demo. The UI hides unavailable controls, but
the backend enforces authority. This is not a production separation-of-duties or
multi-tenant policy implementation.

## 7. Operations and evidence

The root launcher starts AI, backend and frontend in readiness order; a startup
failure rolls back only services started by that command. Native services are
separate from SQL Server's Windows service. Docker Compose is an alternative
runtime; GitHub Actions verifies builds/tests and is not part of a claim request.

Demo token expiry is `now + 3600` Unix seconds (one hour) in
`backend/scripts/Start-Demo.ps1`. Backend restart rotates the signing key, so old
demo tokens must be replaced. Normal identity-provider configuration is separate.

Use existing tests as concrete evidence: `ClaimSecurityTest`, domain/service tests,
`HttpAssessmentClientTest`, Python assessment/model tests and the browser workflow
in `frontend/e2e/claims.spec.ts`. Discuss the scenario each test proves, not only
the count. Real-outcome validation, Azure/Kubernetes deployment and persistent
assessment history remain future work.

## Boundaries of these annotations

The notes cover the main application, transport, domain, security, model and launcher
paths. Applied Flyway migrations and the JSON model artifact are deliberately not
annotated: editing migrations can break checksums, and JSON does not support comments.
The pre-existing local V1 variant and untracked Jupyter work are not changed.
Comments explain the implementation; they are not evidence of independent review,
production compliance or measured business outcomes.
