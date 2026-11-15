# Security boundaries

Spring Boot validates bearer tokens and enforces scopes and claim ownership.
Writers manage their drafts; reviewers can read/review all claims in this
single-tenant prototype. ETags protect against stale writes. Claim changes and
audit metadata commit atomically. Audit rows omit clinical and insurance content.

The explicit loopback demo profile generates short-lived test identities. Never
enable it in production. Normal configuration requires the trusted issuer and
audience. Browser tokens remain in memory; `VITE_*` configuration must not contain
secrets. Test-only signing material in browser tests is not deployment material.

The backend sends only model indicators to Python. Identifiers, clinical codes
and free-text notes are omitted. The Python server binds to loopback; an optional
shared key protects the assessment route. It is a development HTTP server,
not a production hosting or authentication solution.

Before deployment: configure identity and role provisioning, TLS, service
authentication, secrets management, database/audit privileges, retention,
monitoring and security acceptance. This application does not claim HIPAA
certification or authorization to process real patient information. Use synthetic
data for demonstrations. Assessment findings do not replace payer verification
or qualified review.
