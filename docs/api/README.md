# Claim information API

Base route: `/api/v1/claims`. Existing bearer scopes, ownership checks, ETags and
`If-Match` requirements remain unchanged. Swagger is available at `/api-docs`
when the documentation profile is enabled.

Creation supports an optional UUID `Idempotency-Key` header. Reuse the same key
and payload after an uncertain response. A replay returns the existing claim's
current representation and ETag with 201; a different payload with that key
returns 409 `IDEMPOTENCY_CONFLICT`. Keys are scoped to the authenticated subject.
Calls without a key create a new draft on every successful request.

List responses contain claim summaries without `context`. Fetch `/{id}` for the
full insurance, diagnoses, service lines and clinical summary. The frontend uses
one retry key per open creation form; refreshing the form creates a new retry
context, so check the claims list after an uncertain save before starting again.

## Create / update example

```json
{
  "patientReference": "PAT-DEMO-001",
  "providerReference": "PROV-DEMO-001",
  "amount": 125.50,
  "currency": "USD",
  "serviceDate": "2026-01-01",
  "context": {
    "insurance": {
      "payerName": "Demo payer",
      "planReference": "PLAN-DEMO",
      "memberReference": "MEM-DEMO-001",
      "coverageStatus": "UNKNOWN",
      "coverageVerifiedOn": null,
      "authorizationStatus": "NOT_REQUIRED",
      "authorizationReference": null
    },
    "diagnosisCodes": ["Z00.00"],
    "serviceLines": [
      {"codeSystem":"LOCAL","procedureCode":"DEMO-001","description":"Synthetic service","units":1,"unitPrice":125.50}
    ],
    "clinicalSummary": "Synthetic supporting context for the demo"
  }
}
```

`amount` must equal the exact decimal sum of `units * unitPrice`. The UI derives
it automatically; the domain verifies it independently. One to 50 services and
one to 20 distinct diagnosis identifiers are supported. Units are whole numbers
1-999; charges must be positive with at most two fractional digits. Procedure
systems are LOCAL, CPT, or HCPCS. Diagnosis fields are intended for ICD-10-CM
identifiers; current checks enforce basic identifier syntax, not membership in a
clinical code catalog, medical necessity, or coding compatibility.

Coverage: UNKNOWN / ACTIVE / INACTIVE. Authorization: UNKNOWN / NOT_REQUIRED /
PENDING / APPROVED / DENIED. These are user-reported states, not verified payer
responses. APPROVED requires an authorization reference. Coverage check dates
cannot be in the future. Payer name (150), plan/member references (100 each),
service descriptions (200), and optional clinical summary (2,000) have length
limits. Use synthetic data only; do not enter names or identifying clinical notes.

A context block is optional when saving an API draft for backward compatibility.
When supplied it must be complete. Submitting a draft without context returns
400 and leaves it unchanged. The UI collects the complete block before saving.
Historical submitted/reviewed claims remain readable and retain their original
status. Their context is null; the UI explicitly marks missing information.
No historical medical or insurance information is invented by migration V5.

Updates replace the context block and ordered diagnosis/service collections in
the same transaction as the claim and audit event. Root optimistic locking also
protects collection-only edits. Audit rows continue to store only action metadata,
not insurance identifiers or clinical narrative. Assessments use the saved context
through the endpoint below.

## Assess a saved claim

`POST /api/v1/claims/{id}/assessment` takes no request body. Send the bearer token
and current quoted `If-Match` ETag. Claim-read scope and ownership checks run
before inference. Missing context returns 422; missing/malformed/stale ETags
return 428/400/412. A claim changed during inference also returns 412.

The response contains `claimId`, `claimVersion`, and `assessment` with
`modelVersion`, `trainingData`, `assessedAt`, `score` (0-100), `riskLevel`,
`interceptLogOdds`, `findings`, and `limitations`. Each finding includes a code,
field, recorded evidence, log-odds contribution and suggested correction.
Responses are not cached. Assessment does not change status or persist history.

The backend sends coverage/authorization states, coverage check date, service
code systems, and supporting-context presence. It omits identifiers, actual
clinical codes, financial values and clinical narrative. The Python service uses
synthetic outcomes and does not verify coverage, coding or medical necessity.
No result guarantees payer acceptance or denial.

The backend has a 2-second connect timeout and 5-second read timeout. An
unavailable/invalid model response returns 503 with `ASSESSMENT_UNAVAILABLE`;
there is no fallback score. Missing-context errors use the same error shape.
