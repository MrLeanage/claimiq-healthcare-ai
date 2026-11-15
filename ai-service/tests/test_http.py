import http.client
import json
import os
import threading
import unittest
from http.server import ThreadingHTTPServer
from unittest.mock import patch
from app.server import Handler


class HttpTests(unittest.TestCase):
    def test_backend_chunked_transport_and_service_key(self):
        server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            with patch.dict(os.environ, {"CLAIMIQ_AI_API_KEY": "test-key"}):
                connection = http.client.HTTPConnection(
                    *server.server_address, timeout=3
                )
                payload = json.dumps(
                    {
                        "context": {
                            "insurance": {
                                "coverageStatus": "ACTIVE",
                                "coverageVerifiedOn": "",
                                "authorizationStatus": "PENDING",
                            },
                            "diagnosisCodes": ["PRESENT"],
                            "serviceLines": [{"codeSystem": "LOCAL"}],
                            "clinicalSummary": "PRESENT",
                        }
                    }
                ).encode()
                connection.request(
                    "POST",
                    "/assess",
                    iter([payload[:50], payload[50:]]),
                    {"X-AI-Key": "test-key", "Content-Type": "application/json"},
                    encode_chunked=True,
                )
                response = connection.getresponse()
                self.assertEqual(200, response.status)
                self.assertEqual(
                    "SYNTHETIC", json.loads(response.read())["trainingData"]
                )
                connection.close()
                connection = http.client.HTTPConnection(
                    *server.server_address, timeout=3
                )
                connection.request("POST", "/assess", payload)
                response = connection.getresponse()
                self.assertEqual(401, response.status)
                response.read()
                connection.close()
        finally:
            server.shutdown()
            server.server_close()
            thread.join()
