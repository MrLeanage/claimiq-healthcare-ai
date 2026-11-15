# Architecture

## Claims backend

HTTP controllers delegate to the `ClaimService` contract. `DefaultClaimService`
coordinates transactions, the repository, access policy, mapping and audit events.
The domain enforces draft validation and lifecycle transitions independently of
HTTP validation. Spring Data supplies persistence implementations.

```text
REST controllers -> ClaimService -> domain + repository + audit
                         |
                   ClaimAccessPolicy -> JWT adapter
```

The claims feature groups API, DTO, domain, service, repository, mapper, exception
and security packages. Immutable DTO records cross the API boundary; JPA entities
encapsulate persistence state. Updates require the latest ETag and flush before
returning current metadata. Audit records commit in the same transaction.

## Claim context

A validated snapshot contains insurance and authorization information, diagnoses,
ordered service lines, and an optional supporting summary. The amount must match
the billed service total. Drafts need complete context before submission; older
claims remain readable with null context. V5 adds the storage without fabricating
historical medical information.

## Verification

The default suite covers domain invariants, mapping, API validation, scopes,
ownership and concurrency on isolated H2. The `sqlserver-it` profile additionally
checks migration upgrades and relational behavior on disposable SQL Server
databases. Keep the published V1 migration unchanged; use forward migrations.

See [the API contract](../api/README.md) and [backend guide](../../backend/README.md).

## Assessment boundary

```text
Authenticated assessment endpoint -> ClaimService (scope + ownership + version)
                                  -> Python HTTP service -> saved logistic model
```

The backend extracts only the indicators used by this model. Patient, provider,
plan and member references, code identifiers, and narrative text do not cross
this boundary. Timeouts and service failures return an unavailable state. A
second claim-version check rejects results computed while the saved claim changed.

The Python runtime has no third-party dependencies. Training generates explicit
synthetic outcomes and learns weights; inference loads a versioned JSON artifact.
Contributions are exact on the log-odds scale. Corrective actions come from a
curated catalogue. These are advisory findings, not payer verification.
Assessment persistence and external outcome validation are not implemented.
