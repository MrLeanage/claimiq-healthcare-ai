# Starting and stopping ClaimIQ

From the **solution root** (the folder containing backend and frontend), run in PowerShell:

```powershell
.\dev.ps1 start
.\dev.ps1 status
.\dev.ps1 tokens
.\dev.ps1 stop
.\dev.ps1 restart
```

| Command | Purpose |
| --- | --- |
| `start` | Start backend, wait for health, then start frontend. Return when ready. |
| `status` | Show state, ports, PIDs, URLs, and log folders. |
| `tokens` | Print current managed backend writer/reviewer demo tokens. |
| `stop` | Stop both application process trees. SQL Server stays running. |
| `restart` | Stop both and start them again in dependency order. |
| `logs` | Show recent stdout/stderr and log paths. |

Servers run in the background without extra terminal windows. Repeated `start`
does not create duplicates; repeated `stop` is safe. SQL records survive restart.
Demo tokens expire after one hour and rotate on every backend restart. Reconnect
the frontend using the fresh `tokens` output after restarting the backend.

Use `-Service backend` or `-Service frontend` to manage just one server:

```powershell
.\dev.ps1 restart -Service frontend
.\dev.ps1 restart -Service backend
```

## Project-level commands

Inside backend, use `.\dev.ps1 start`, `stop`, `restart`, `status`, `logs`, or
`tokens`. Inside frontend, use the same commands except `tokens`. Each manages
only its own component. Existing build/test commands are unchanged.

The foreground commands `backend/dev.ps1 demo` and `frontend/dev.ps1 dev` remain
available. **Ctrl+C** gives a graceful foreground shutdown. The manager can
recognize repository-specific foreground servers on its selected ports and stop
them when switching to managed startup. For tokens from an unmanaged instance,
use its original terminal, or restart it through the manager.

## Setup and settings

Run `frontend/dev.ps1 install` once. Java 17 and Node/npm are required; the
frontend installation script supports portable npm if only Node is available.
Configure the matching Windows authentication DLL directory once with
`backend/scripts/Configure-SqlServer.ps1`; see [backend commands](../backend/scripts/README.md).

If Windows PowerShell blocks local scripts, a process-only invocation is
`powershell -NoProfile -ExecutionPolicy Bypass -File .\dev.ps1 start`.
This does not change the machine or user execution policy permanently.

Defaults are SQL Server storage, backend on 8081, and frontend on 5173:

```powershell
.\dev.ps1 start -JavaHome 'C:\path\to\jdk-17'
.\dev.ps1 restart -BackendPort 8082 -FrontendPort 5175
```

Settings are remembered per repository/component. Solution-level startup points
the frontend proxy at the selected backend port. When managing the frontend
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

Failed startup reports its log folder. Readiness waits up to 90 seconds and cleans
up timed-out starts. No Windows services, scheduled tasks, permanent PATH changes,
or automatic restarts are installed.
