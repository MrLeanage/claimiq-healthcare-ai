import base64
import contextlib
import io
from pathlib import Path
import tempfile
import unittest
from demo import initialize, load_settings, make_token


class DemoSettingsTest(unittest.TestCase):
    def test_initialization_preserves_existing_credentials(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / ".env"
            with contextlib.redirect_stdout(io.StringIO()):
                initialize(path, 5173)
            original = path.read_bytes()
            settings = load_settings(path)
            self.assertEqual(
                32, len(base64.b64decode(settings["CLAIMIQ_DEMO_JWT_SECRET"]))
            )
            with self.assertRaises(SystemExit):
                initialize(path, 5174)
            self.assertEqual(original, path.read_bytes())

    def test_short_signing_key_is_rejected(self):
        with self.assertRaises(ValueError):
            make_token(
                {"CLAIMIQ_DEMO_JWT_SECRET": base64.b64encode(b"short").decode()},
                "demo",
                "claims:read",
            )


if __name__ == "__main__":
    unittest.main()
