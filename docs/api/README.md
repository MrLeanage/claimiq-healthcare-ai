# Claim information API

Base route: `/api/v1/claims`. Existing bearer scopes, ownership checks, ETags and
`If-Match` requirements remain unchanged. Swagger is available at `/api-docs`
when the documentation profile is enabled.

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
not insurance identifiers or clinical narrative. There is no AI assessment call
or generated risk score in this change; these are inputs for a later assessment.
