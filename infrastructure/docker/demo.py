"""Local Compose credentials and short-lived demo tokens. Standard library only."""

import argparse
import base64
import hashlib
import hmac
import json
import os
from pathlib import Path
import secrets
import time

ROOT = Path(__file__).resolve().parents[2]


def load_settings(path):
    settings = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.strip() and not line.lstrip().startswith("#"):
            name, value = line.split("=", 1)
            settings[name.strip()] = value.strip()
    return settings


def initialize(path, port):
    values = {
        "CLAIMIQ_WEB_PORT": str(port),
        "CLAIMIQ_SQL_PASSWORD": "Ci1!" + secrets.token_urlsafe(24),
        "CLAIMIQ_DEMO_JWT_SECRET": base64.b64encode(secrets.token_bytes(32)).decode(),
        "CLAIMIQ_AI_API_KEY": secrets.token_urlsafe(32),
    }
    try:
        descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    except FileExistsError:
        raise SystemExit(
            f"{path.name} already exists; existing settings were preserved."
        )
    with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("# Local Compose demo settings; do not commit or share.\n")
        handle.writelines(f"{name}={value}\n" for name, value in values.items())
    print(
        f"Created {path.name} with unique demo credentials. No existing database was changed."
    )


def make_token(settings, subject, scopes):
    def encode(value):
        return base64.urlsafe_b64encode(value).rstrip(b"=")

    now = int(time.time())
    header = encode(b'{"alg":"HS256","typ":"JWT"}')
    payload = encode(
        json.dumps(
            {
                "sub": subject,
                "scope": scopes,
                "iss": "claimiq-demo",
                "aud": ["claimiq-api"],
                "iat": now,
                "exp": now + 3600,
            }
        ).encode()
    )
    unsigned = header + b"." + payload
    secret = base64.b64decode(settings["CLAIMIQ_DEMO_JWT_SECRET"], validate=True)
    if len(secret) < 32:
        raise ValueError("Demo signing key must contain at least 32 bytes")
    signature = encode(hmac.new(secret, unsigned, hashlib.sha256).digest())
    return (unsigned + b"." + signature).decode()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("task", choices=("init", "tokens"))
    parser.add_argument("--env-file", type=Path, default=ROOT / ".env")
    parser.add_argument("--port", type=int, default=5173)
    args = parser.parse_args()
    if not 1 <= args.port <= 65535:
        parser.error("Port must be between 1 and 65535")
    if args.task == "init":
        initialize(args.env_file, args.port)
    else:
        settings = load_settings(args.env_file)
        for label, subject, scope in (
            ("Writer", "demo-writer", "claims:read claims:write"),
            ("Reviewer", "demo-reviewer", "claims:read claims:review"),
        ):
            print(
                f"{label} (valid for one hour):\n{make_token(settings, subject, scope)}\n"
            )


if __name__ == "__main__":
    main()
