"""Verify the Compose stack through Nginx without printing credentials or claim data."""

import argparse
from datetime import date, timedelta
import json
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from demo import ROOT, load_settings, make_token


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check-persistence", action="store_true")
    args = parser.parse_args()
    settings = load_settings(ROOT / ".env")
    base = "http://127.0.0.1:" + settings.get("CLAIMIQ_WEB_PORT", "5173")
    writer = make_token(settings, "demo-writer", "claims:read claims:write")
    reviewer = make_token(settings, "demo-reviewer", "claims:read claims:review")

    def call(path, body=None, token=writer, etag=None, method=None):
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = "Bearer " + token
        if etag:
            headers["If-Match"] = etag
        req = Request(
            base + path,
            method=method,
            data=json.dumps(body).encode() if body is not None else None,
            headers=headers,
        )
        with urlopen(req, timeout=15) as response:
            content = response.read()
            return response.status, content, response.headers

    record = ROOT / ".docker" / "smoke.json"
    if args.check_persistence:
        claim_id = json.loads(record.read_text())["claimId"]
        for attempt in range(60):
            try:
                status, body, _ = call("/api/v1/claims/" + claim_id)
                if status == 200 and json.loads(body)["status"] == "APPROVED":
                    print("PASS: claim and reviewer decision survived backend restart")
                    return
            except (URLError, HTTPError):
                pass
            time.sleep(1)
        raise RuntimeError("Persisted claim was not available after restart")

    for path in ("/", "/claims", "/claims/direct-link-demo"):
        status, page, _ = call(path, token=None)
        assert (
            status == 200 and b'<div id="root">' in page
        ), "Frontend SPA fallback failed"
    try:
        call("/api/v1/claims", token=None)
        raise AssertionError("Anonymous claims request succeeded")
    except HTTPError as error:
        assert error.code == 401, "Expected authenticated API"

    claim = {
        "patientReference": "PAT-COMPOSE-SYNTHETIC",
        "providerReference": "PROV-COMPOSE",
        "amount": 125,
        "currency": "USD",
        "serviceDate": str(date.today() - timedelta(days=1)),
        "context": {
            "insurance": {
                "payerName": "Demo payer",
                "planReference": "PLAN-COMPOSE",
                "memberReference": "MEM-COMPOSE",
                "coverageStatus": "INACTIVE",
                "authorizationStatus": "PENDING",
            },
            "diagnosisCodes": ["Z00.00"],
            "clinicalSummary": "Synthetic context",
            "serviceLines": [
                {
                    "codeSystem": "LOCAL",
                    "procedureCode": "DEMO-001",
                    "description": "Synthetic service",
                    "units": 1,
                    "unitPrice": 125,
                }
            ],
        },
    }
    status, body, headers = call("/api/v1/claims", claim)
    assert status == 201, "Claim creation failed"
    saved = json.loads(body)
    path = "/api/v1/claims/" + saved["id"]
    etag = headers["ETag"]
    _, body, _ = call(path + "/assessment", {}, etag=etag)
    assessment = json.loads(body)["assessment"]
    assert (
        assessment["riskLevel"] == "HIGH" and assessment["trainingData"] == "SYNTHETIC"
    )
    assert any(f["code"] == "authorization_pending" for f in assessment["findings"])
    assert call(path + "/submit", {}, etag=etag)[0] == 200
    _, _, headers = call(path, token=reviewer)
    _, _, headers = call(
        path + "/status", {"status": "UNDER_REVIEW"}, reviewer, headers["ETag"], "PATCH"
    )
    _, body, _ = call(
        path + "/status", {"status": "APPROVED"}, reviewer, headers["ETag"], "PATCH"
    )
    assert json.loads(body)["status"] == "APPROVED"
    record.parent.mkdir(exist_ok=True)
    record.write_text(json.dumps({"claimId": saved["id"]}), encoding="utf-8")
    print(
        "PASS: SPA routes, authentication, create, model assessment, submission and human review"
    )


if __name__ == "__main__":
    main()
