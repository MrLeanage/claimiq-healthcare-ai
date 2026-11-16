"""Development API; loopback by default, service key required for container binding."""

import argparse
import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from hmac import compare_digest
from app.ml.model import load_model
from app.services.assessment import assess


class Handler(BaseHTTPRequestHandler):
    # Validate/load once at startup. Replacing the artifact requires a server restart.
    model = load_model()

    def log_message(self, *_):
        pass  # Do not log claim bodies, references, or credentials.

    def reply(self, code, body):
        data = json.dumps(body, allow_nan=False).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        if self.path == "/health":
            self.reply(200, {"status": "UP", "modelVersion": self.model["version"]})
        else:
            self.reply(404, {"message": "Not found"})

    def do_POST(self):
        self.connection.settimeout(5)
        if self.path != "/assess":
            return self.reply(404, {"message": "Not found"})
        key = os.environ.get("CLAIMIQ_AI_API_KEY", "")
        if key and not compare_digest(self.headers.get("X-AI-Key", ""), key):
            return self.reply(401, {"message": "Unauthorized"})
        try:
            # Java may stream chunked JSON. Both body paths enforce a 64 KiB limit.
            if self.headers.get("Transfer-Encoding", "").lower() == "chunked":
                body = bytearray()
                while True:
                    size = int(self.rfile.readline(128).strip().split(b";")[0], 16)
                    if size < 0 or len(body) + size > 65536:
                        return self.reply(413, {"message": "Invalid body size"})
                    if size == 0:
                        if self.rfile.readline(128) != b"\r\n":
                            raise ValueError("Trailers are unsupported")
                        break
                    chunk = self.rfile.read(size)
                    if len(chunk) != size or self.rfile.read(2) != b"\r\n":
                        raise ValueError("Invalid chunk")
                    body.extend(chunk)
            else:
                length = int(self.headers.get("Content-Length", "0"))
                if not 0 < length <= 65536:
                    return self.reply(413, {"message": "Invalid body size"})
                body = self.rfile.read(length)
            # Transport bytes -> Python data -> shared assessment logic -> JSON response.
            payload = json.loads(body)
            self.reply(200, assess(payload, self.model))
        except (ValueError, TypeError, KeyError):
            self.reply(422, {"message": "Complete, valid claim context is required"})


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8090)
    args = parser.parse_args()
    print(
        f"ClaimIQ synthetic assessment service: http://{args.host}:{args.port}",
        flush=True,
    )
    if args.host not in ("127.0.0.1", "localhost") and not os.environ.get(
        "CLAIMIQ_AI_API_KEY"
    ):
        parser.error("CLAIMIQ_AI_API_KEY is required when listening beyond loopback")
    ThreadingHTTPServer((args.host, args.port), Handler).serve_forever()
