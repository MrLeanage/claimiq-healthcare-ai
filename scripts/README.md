# Starting and stopping ClaimIQ

From the **solution root**, run in PowerShell or Command Prompt:

```powershell
.\dev.cmd start
.\dev.cmd status
.\dev.cmd tokens
.\dev.cmd stop
.\dev.cmd restart
```

| Command | Purpose |
| --- | --- |
| `start` | Start AI, backend, then frontend; wait for health, display status and print tokens. |
| `status` | Show state, ports, PIDs, URLs, and log folders. |
| `tokens` | Print current managed backend writer/reviewer demo tokens. |
| `stop` | Stop frontend, backend, then AI. SQL Server stays running. |
| `restart` | Stop all three and start them in dependency order; print fresh tokens. |
| `logs` | Show recent stdout/stderr and log paths. |

Servers run in the background without extra terminal windows. Repeated `start`
does not create duplicates; repeated `stop` is safe. SQL records survive restart.
Demo tokens expire after one hour and rotate on every backend restart. Reconnect
the frontend using the fresh `tokens` output after restarting the backend.

Use `-Service ai`, `backend` or `frontend` to manage just one server:

```powershell
.\dev.cmd restart -Service ai
.\dev.cmd restart -Service backend
.\dev.cmd logs -Service frontend
```

## Project-level commands

Inside backend, use `.\dev.ps1 start`, `stop`, `restart`, `status`, `logs`, or
`tokens`. Inside frontend, use the same commands except `tokens`. Each manages
only its own component. Inside `ai-service`, `dev.ps1` supports the same lifecycle
commands except tokens, plus its existing serve/train/test tasks. Existing
build/test commands are unchanged.

The foreground commands `backend/dev.ps1 demo` and `frontend/dev.ps1 dev` remain
available. **Ctrl+C** gives a graceful foreground shutdown. The manager can
recognize repository-specific foreground servers on its selected ports and stop
them when switching to managed startup. A Python process started with
`python -m app.server` cannot be proven to belong to this repository from its
command line, so close that original terminal process with Ctrl+C first. The
manager refuses occupied/unrecognized ports instead of stopping them. For tokens
from an unmanaged backend, use its terminal or restart it through the manager.

## Setup and settings

Run `frontend/dev.ps1 install` once. Python 3.12+, Java 17 and Node/npm are required; the
frontend installation script supports portable npm if only Node is available.
Configure the matching Windows authentication DLL directory once with
`backend/scripts/Configure-SqlServer.ps1`; see [backend commands](../backend/scripts/README.md).

The `dev.cmd` wrapper uses Windows PowerShell with a process-only execution-policy
override; it does not change user or machine policy. `dev.ps1` remains available
for callers whose policy permits scripts. The launcher does not install tools.

Defaults are SQL Server storage, AI on 8090, backend on 8081 and frontend on 5173:

```powershell
.\dev.cmd start -JavaHome 'C:\path\to\jdk-17'
.\dev.cmd start -Database Memory
.\dev.cmd stop
.\dev.cmd start -AiPort 8092 -BackendPort 8082 -FrontendPort 5175
```

Settings are remembered per repository/component. Solution-level startup points
the frontend proxy at the selected backend port and the backend at the selected
AI port. Stop an existing managed service before changing its port. Use
`-PythonExe 'C:\path\to\python.exe'` to select an interpreter. When managing the frontend
independently, use `frontend/dev.ps1 start -BackendUrl http://127.0.0.1:8082` if needed.

Use `-Database Memory` for disposable H2 data, and `-Database SqlServer` to switch
back. This choice is remembered. The manager does not delete SQL records, repair
migration history, or stop the SQL Server service. Normal Flyway migrations still
run on backend startup.

## Ownership, logs, and shutdown

Runtime files are outside the repository under
`%LOCALAPPDATA%\ClaimIQ\servers\<repository-key>\<component>`; `status` prints the
exact path. Logs and token files contain demo credentials: keep them local. The
signing key is not written into the configuration. Current token files are removed
on managed stop; tokens in old logs become invalid when the signing key rotates.

The manager checks worker command lines and process creation timestamps before
stopping a process tree. Foreground processes must match this repository's Spring
Boot classpath or Vite executable. A port owned by another application is refused,
so unrelated Java/Node applications are not stopped. Commands are serialized per
component to prevent duplicate managed starts.

On Windows, managed stop terminates the verified development process tree. Use
foreground commands and Ctrl+C when graceful application shutdown is needed.
Committed SQL data remains durable; SQL Server rolls back incomplete transactions.

Failed startup reports its log folder. Readiness waits up to 300 seconds per
service (override with `-StartupTimeoutSeconds`, up to 600), allowing for Maven
compilation and SQL Server startup on a busy machine. Frontend readiness checks
the displayed localhost URL, supporting either IPv4 or IPv6 binding. The manager cleans
up timed-out starts. A failed root start rolls back only services it newly started;
already-running services are preserved. Stop attempts every component and reports
any occupied/unrecognized ports. Root commands are serialized to avoid overlapping
stack operations. No Windows services, scheduled tasks, permanent PATH changes or
automatic restarts are installed.

## Verification

`Test-ServerLifecycle.ps1` exercises startup, repeated start, actual assessment
through custom ports, token rotation on restart, repeated stop and rollback on an
unrelated occupied port. Use a clean checkout: it runs disposable H2 on ports
18081/18090/15173 and stops only its managed processes. Windows CI runs it with
Windows PowerShell, Java 17, Python 3.12 and Node 24. Tokens are captured rather
than published in test output.
