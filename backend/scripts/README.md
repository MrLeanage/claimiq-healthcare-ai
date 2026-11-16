# Reusable backend commands

Inside backend, use `.\dev.ps1 start`, `status`, `stop`, `restart`, `logs`, and
`tokens` for background server management. From the solution root the same commands
manage all three servers. Root `dev.cmd start` also prints the demo tokens.
See [server lifecycle commands](../../scripts/README.md).
The existing `demo` task remains a foreground option stopped with Ctrl+C.

From inside `backend`, use `.\dev.ps1 <task>`. From the repository root, use
`.\backend\dev.ps1 <task>` instead. Paths resolve from the script, so commands do not
require changing your current directory. Existing JAVA_HOME is used; optionally
pass `-JavaHome 'C:\path\to\jdk-17'` for this invocation only.

| Task | Example from inside backend | Result |
| --- | --- | --- |
| Help | `.\dev.ps1 help` | Show tasks and options |
| Environment | `.\dev.ps1 info` | Java and Maven versions |
| Default tests | `.\dev.ps1 test` | Unit/API/security tests on isolated H2 |
| Full build | `.\dev.ps1 verify` | Clean, test, package executable JAR |
| Package | `.\dev.ps1 package` | Package JAR with default tests |
| Dependencies | `.\dev.ps1 dependencies` | Resolve Maven dependencies |
| Clean | `.\dev.ps1 clean` | Delete generated backend/target via Maven |
| Demo | `.\dev.ps1 demo` | Persistent SQL Server, demo tokens, localhost:8081 Swagger |
| Custom demo port | `.\dev.ps1 demo -Port 8082` | Use a different port |
| Local SQL Server | `.\dev.ps1 local -Docs` | Existing local profile, Swagger on port 8080 |
| SQL integration tests | `.\dev.ps1 sql-test` | Default tests plus real SQL Server gate |

The existing `scripts\Start-Demo.ps1` also remains usable from backend. Stop
foreground servers with Ctrl+C. Demo tokens expire after an hour; restart for
new tokens. SQL Server records survive shutdown. Use `.\dev.ps1 demo -Database Memory` for a disposable H2 demo instead.

## Persistent demo setup

The default `demo` task starts `demo,demo-sqlserver` against the existing
`ClaimIQ` SQL Server database using Windows authentication, on port 8081.
It still uses demo JWTs; a real identity provider is not required for this mode.
Native authentication uses the identity running Java.

Set the matching x64 JDBC DLL folder once (inside backend):

```powershell
.\scripts\Configure-SqlServer.ps1 -AuthLibraryPath 'D:\your-jdbc-auth-folder\x64'
.\dev.ps1 demo
```

The folder is saved for your Windows user at `%LOCALAPPDATA%\ClaimIQ\backend.json`.
No credentials are stored. Alternatively pass `-AuthLibraryPath` each run or set
`CLAIMIQ_SQL_AUTH_DIR`. The matching driver/DLL version is required. The script
passes the native path through temporary JAVA_TOOL_OPTIONS so spaces work and
restores that environment variable on exit. `CLAIMIQ_DEMO_DB_URL` can override the
Windows-authentication development database URL.

For disposable data: `.\dev.ps1 demo -Database Memory`. This starts only the
`demo` profile on H2, as used by isolated browser tests. Direct `demo` profile
activation also uses H2; the startup script adds the SQL Server overlay by default.

## Windows authentication

Use the actual folder containing the matching Microsoft JDBC authentication DLL:

```powershell
.\dev.ps1 local -Docs -AuthLibraryPath 'D:\your-jdbc-auth-folder\x64'
```

The local profile still uses your current SQL Server connection settings. Startup
may apply configured Flyway migrations; preserve the local V1 file matching its database history. The scripts do not repair
migration history or bypass JWT validation. Local API calls require the configured
issuer, audience, and a valid provider token; use `demo` for the standalone demo.

## SQL Server integration gate

Set these in your terminal using a dedicated development/test SQL Server account:

```powershell
$env:CLAIMIQ_IT_ADMIN_URL = 'jdbc:sqlserver://localhost:1433;databaseName=master;encrypt=true;trustServerCertificate=true'
$env:CLAIMIQ_IT_USERNAME = '<test-admin>'
$env:CLAIMIQ_IT_PASSWORD = '<test-password>'
.\dev.ps1 sql-test
```

For Windows authentication, use an integrated-security URL, leave username and
password unset, and pass the DLL directory:

```powershell
$env:CLAIMIQ_IT_ADMIN_URL = 'jdbc:sqlserver://localhost:1433;databaseName=master;integratedSecurity=true;encrypt=true;trustServerCertificate=true'
.\dev.ps1 sql-test -AuthLibraryPath 'D:\your-jdbc-auth-folder\x64'
```

The account needs CREATE DATABASE permission. Tests create and drop a uniquely
named `ClaimIQ_it_*` database; never use a production instance. Credentials stay
in environment variables, not scripts. Build output is in `backend/target`;
Surefire reports are in `target/surefire-reports`, and SQL test reports are in
`target/failsafe-reports`.

Nonzero Maven exits fail the script. Working directory and any JavaHome override
are restored when it finishes. Scripts do not install software or alter permanent
machine settings.

Demo endpoints use `/api/v1/claims`; the OpenAPI document is `/api-docs`.
Swagger UI remains at `/swagger-ui/index.html`. Restart the backend after URL changes.
