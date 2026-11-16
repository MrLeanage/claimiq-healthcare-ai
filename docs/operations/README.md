# Operations

See [server lifecycle commands](../../scripts/README.md) for local startup, stop,
restart, ports, token retrieval and logs. `dev.cmd start` controls Python, Spring
Boot and Vite, shows readiness and prints tokens. SQL Server remains running
when application processes stop.

| Component | Default address | Check |
| --- | --- | --- |
| Frontend | http://127.0.0.1:5173 | Open the workspace |
| Backend demo | http://127.0.0.1:8081 | `/actuator/health` |
| Assessment service | http://127.0.0.1:8090 | `/health` |

Restarting the backend rotates demo tokens. Reconnect the frontend with fresh
tokens. SQL Server records persist; H2 demo records do not. Restarting Python
reloads the saved model, without training. The Python root URL returns 404 by design.

If assessment returns 503, check the Python health endpoint and backend AI URL/key.
For 412, reload the claim and assess the latest version. Do not resolve Flyway
checksum errors by blindly repairing history; preserve local configuration and
reconcile the schema deliberately. Assessment history is not persisted.

See [Docker operations](../../infrastructure/docker/README.md) for an isolated
demonstration of all services. Native scripts remain available. Cloud deployment
and production monitoring remain planned work.
