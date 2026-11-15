# ClaimIQ architecture and OOP/SOLID review

## Scope

The implemented application is currently the Java claims backend. The frontend,
AI service, and infrastructure directories contain scaffolding only; they have
no implemented class design to assess yet.

## Responsibilities and dependency boundaries

```text
HTTP controller → ClaimService interface → DefaultClaimService
                                            ├── ClaimRepository interface
                                            ├── ClaimNumberGenerator interface
                                            ├── ClaimMapper
                                            └── Claim domain behavior
```

The domain enforces lifecycle transitions and rejects edits after submission.
The service coordinates use cases and transactions. Spring Data implements the
repository contract. The mapper translates representations, and the HTTP advice
translates exceptions into the immutable `ApiError` response contract.

| Principle | Current application |
| --- | --- |
| Single responsibility | HTTP handling, orchestration, numbering policy, mapping, persistence, and lifecycle rules have separate owners. |
| Open/closed | Alternate service or number-generator implementations can be injected without changing callers. A changed business workflow still requires an explicit domain-rule change. |
| Liskov substitution | Callers use service/repository/numbering contracts. Replacement implementations must preserve the documented results and failures; interfaces alone do not prove substitutability. |
| Interface segregation | The numbering contract has one operation, and the service exposes only six claim use cases. The standard Spring Data repository is retained rather than adding a duplicate persistence abstraction. |
| Dependency inversion | Controllers depend on `ClaimService`; orchestration receives repository and numbering contracts through constructor injection. |

## Basic OOP

- Encapsulation: entity fields are private, lifecycle state has no public raw
  setters, and non-draft detail edits are rejected by the domain.
- Abstraction: use-case, persistence, and numbering contracts hide their
  implementation choices.
- Polymorphism: Spring supplies implementations through those contracts; tests
  substitute mocks and a deterministic numbering policy.
- Composition: controller, service, mapper, and repository collaborate without
  a custom inheritance hierarchy. Inheritance is not required merely to claim
  OOP compliance; the existing framework and exception inheritance is retained.
- DTOs are immutable records; JPA entities remain mutable where persistence and
  domain operations require it.

`ClaimMapper` remains a simple concrete collaborator. There is no second mapping
policy or independent boundary that warrants an interface and implementation pair.
State transitions remain explicit domain rules rather than introducing a state
class hierarchy for a five-state workflow.

## Automated safeguards and limits

- Domain tests cover every status pair, repeat submission, null targets, and
  rejected edits without state mutation.
- Service tests exercise lifecycle orchestration through `ClaimService` and a
  replaceable number generator.
- Spring context tests verify component wiring and entity/repository discovery.
- HTTP tests exercise valid creation, input validation, pagination rejection,
  malformed JSON, missing claims, and transition-conflict responses.
- Architecture tests guard interface dependencies, constructor-injected final
  collaborators, immutable DTOs, and the absence of raw lifecycle setters.

These checks provide evidence for the current design; they are not a formal
guarantee that every future change satisfies SOLID. Runtime SQL Server mapping,
migrations, concurrency, authorization, and end-to-end behavior require their own
integration and security verification.
