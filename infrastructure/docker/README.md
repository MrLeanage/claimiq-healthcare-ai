# Docker Compose demo

The stack runs the built React app behind Nginx, Spring Boot, the Python model
and an isolated SQL Server database. A one-shot initializer creates the database
before Spring applies the published Flyway migrations. Only the web interface
is published on the host's loopback address. Existing Windows databases and
native application processes are not used.

## Requirements

- Docker Desktop running Linux containers, or Docker Engine with Compose v2.
- An x86-64 host compatible with the Microsoft SQL Server Linux image.
- Python 3.12+ for local settings and tokens.
- Available host port 5173, or a different port chosen during initialization.

SQL Server Developer is for development/demo use and requires accepting
Microsoft's image license. Demo signing keys, the SQL administrator account and
the Python development server make this a local demonstration configuration.

## Start and demonstrate

From the repository root:

```powershell
python infrastructure/docker/demo.py init
docker compose up -d --build --wait
python infrastructure/docker/demo.py tokens
```

Open http://localhost:5173 and connect with the writer token. Create a synthetic
claim, assess it, correct the draft and reassess. Submit it, then reconnect with
the reviewer token to complete review. Docker Desktop provides a separate view
of containers, health and logs when discussing architecture.

Initialization creates ignored `.env` credentials and refuses to overwrite an
existing file. If Vite occupies port 5173, use `init --port 5175` before the first
startup, or edit only `CLAIMIQ_WEB_PORT` in `.env`; then open port 5175. The first
build downloads images and dependencies and may take several minutes.

Compose waits for database and model health, successful database initialization,
and backend readiness before starting the frontend. Health checks gate startup;
`restart: unless-stopped` restarts exited processes, not merely unhealthy ones.
The app reports runtime dependency failures instead of returning a fallback score.

## Manage the stack

```powershell
docker compose ps --all
docker compose logs -f backend ai-service
docker compose stop
docker compose start
docker compose down
```

`stop` and `down` preserve the named SQL volume. The next `up` recreates containers
with existing records. **Adding `--volumes` to `down` deletes the demo database.**
Only disposable CI cleanup uses that option. Database and backend ports are not
published separately.

Tokens expire after one hour; run the tokens command again without restarting.
Docker backend restarts keep the signing key from `.env`. Do not regenerate or
change the SQL password while keeping an existing database volume: the database
retains its original password. Keep `.env` private and retain it with the volume.
Do not show tokens or `.env` contents in interview slides or public logs.

## Verify

```powershell
python -m unittest discover -s infrastructure/docker -p "test_*.py" -v
docker compose config --quiet
python infrastructure/docker/smoke.py
docker compose restart backend
python infrastructure/docker/smoke.py --check-persistence
```

The smoke test checks Nginx SPA fallback, authentication, creation, assessment,
submission and reviewer approval. It writes a synthetic claim and stores its ID
in ignored `.docker/` for the restart check. It prints no tokens or claim data.
GitHub Actions builds the containers and checks persistence using a disposable
volume. No existing native SQL Server database is involved.

## Layout and deployment boundaries

Dockerfiles and Nginx configuration live here. The root `docker-compose.yml`
defines networking, settings, health and storage. Build contexts exclude secrets,
local database configuration, dependencies and generated output. Runtime images
contain built assets/JAR or Python inference code, without build toolchains.
Application containers use non-root users.

Environment-based credentials are for this local demo. Production requires
governed identity, secret storage, least-privilege database roles, TLS and a
production Python server. Azure/Kubernetes deployment remains separate work.
