"""Evidence-backed findings, exact log-odds contributions, curated next actions."""

from datetime import date, datetime, timezone
from app.ml.model import FEATURES, probability

CATALOGUE = {
    "coverage_inactive": (
        "Reported coverage is inactive",
        "insurance.coverageStatus",
        "Verify coverage for the service date with the payer and correct the recorded status only when confirmed.",
    ),
    "coverage_unknown": (
        "Coverage status is unknown",
        "insurance.coverageStatus",
        "Obtain an eligibility response for the service date and record the verified status.",
    ),
    "coverage_unverified": (
        "No coverage verification date recorded",
        "insurance.coverageVerifiedOn",
        "Verify eligibility with the payer and record the actual check date; a date alone does not establish coverage.",
    ),
    "authorization_denied": (
        "Reported authorization is denied",
        "insurance.authorizationStatus",
        "Review the payer authorization response and resolve the denial with the responsible team.",
    ),
    "authorization_pending": (
        "Authorization is pending",
        "insurance.authorizationStatus",
        "Confirm whether authorization is required and obtain the payer response before submission.",
    ),
    "authorization_unknown": (
        "Authorization requirements are unknown",
        "insurance.authorizationStatus",
        "Check the payer and plan authorization requirements for the billed services.",
    ),
    "local_codes": (
        "Local service codes need mapping review",
        "serviceLines.codeSystem",
        "Ask a qualified coder to verify the appropriate submission codes against the source documentation.",
    ),
    "summary_missing": (
        "No supporting summary recorded",
        "clinicalSummary",
        "Review the source documentation and add relevant supporting context if appropriate; do not invent clinical details.",
    ),
}


def assess(payload, model):
    if not isinstance(payload, dict) or not isinstance(payload.get("context"), dict):
        raise ValueError("Complete claim context is required")
    context = payload["context"]
    insurance = context.get("insurance")
    if not isinstance(insurance, dict):
        raise ValueError("Insurance details are required")
    coverage, authorization = insurance.get("coverageStatus"), insurance.get(
        "authorizationStatus"
    )
    if coverage not in ("ACTIVE", "INACTIVE", "UNKNOWN") or authorization not in (
        "UNKNOWN",
        "NOT_REQUIRED",
        "PENDING",
        "APPROVED",
        "DENIED",
    ):
        raise ValueError("Invalid coverage or authorization status")
    lines = context.get("serviceLines")
    if (
        not isinstance(lines, list)
        or not 1 <= len(lines) <= 50
        or any(
            not isinstance(line, dict)
            or line.get("codeSystem") not in ("LOCAL", "CPT", "HCPCS")
            for line in lines
        )
    ):
        raise ValueError("Valid service lines are required")
    if (
        not isinstance(context.get("diagnosisCodes"), list)
        or not context["diagnosisCodes"]
    ):
        raise ValueError("Diagnoses are required")
    verified = insurance.get("coverageVerifiedOn")
    if verified:
        date.fromisoformat(verified)
    values = [
        int(coverage == "INACTIVE"),
        int(coverage == "UNKNOWN"),
        int(not verified),
        int(authorization == "DENIED"),
        int(authorization == "PENDING"),
        int(authorization == "UNKNOWN"),
        int(any(l["codeSystem"] == "LOCAL" for l in lines)),
        int(not str(context.get("clinicalSummary") or "").strip()),
    ]
    findings = []
    for name, value, weight in zip(FEATURES, values, model["weights"]):
        if value:
            title, field, correction = CATALOGUE[name]
            evidence = (
                coverage
                if name.startswith("coverage_") and name != "coverage_unverified"
                else (
                    authorization
                    if name.startswith("authorization_")
                    else "LOCAL" if name == "local_codes" else "Not recorded"
                )
            )
            findings.append(
                dict(
                    code=name,
                    title=title,
                    field=field,
                    evidence=evidence,
                    contributionLogOdds=weight,
                    suggestedCorrection=correction,
                )
            )
    findings.sort(key=lambda f: f["contributionLogOdds"], reverse=True)
    score = probability(model, values)
    return dict(
        modelVersion=model["version"],
        trainingData="SYNTHETIC",
        assessedAt=datetime.now(timezone.utc).isoformat(),
        score=round(score * 100, 1),
        riskLevel="HIGH" if score >= 0.65 else "MEDIUM" if score >= 0.30 else "LOW",
        interceptLogOdds=model["intercept"],
        findings=findings,
        limitations=[
            "Demonstration score learned from synthetic outcomes; not a validated denial probability.",
            "Inputs are user-reported, not payer-verified. No coding or medical-necessity validation is performed.",
            "No duplicate-claim, timely-filing, payer-rule or historical-outcome analysis is available.",
            "Contributions explain the model output in log-odds; they do not establish causation or guarantee a correction will prevent denial.",
        ],
    )
