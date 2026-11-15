# ClaimIQ synthetic denial-risk demonstration

Python 3.12+, standard library only. From the repository root:

```powershell
.\ai-service\dev.ps1 test
.\ai-service\dev.ps1 serve
```

The server binds to `127.0.0.1:8090`. Keep this terminal open. Start/restart the
backend with the existing app commands. When the frontend is installed, open a
saved claim and select **Assess denial risk**. The frontend calls the authenticated
backend, never Python directly.
The backend default AI URL is `http://127.0.0.1:8090`; override with
`CLAIMIQ_AI_BASE_URL` (Spring relaxed binding for `claimiq.ai.base-url`). For a shared
service key, set `CLAIMIQ_AI_API_KEY` in both processes. This is a loopback development
server; deployment requires a production server, TLS and service authentication.

If PowerShell blocks scripts, run `python -m app.server --port 8090` from
`ai-service`. Check `http://127.0.0.1:8090/health`; `/` has no homepage.
Restarting Spring Boot does not retrain or restart Python. Restarting Python
reloads the saved model artifact.

For development formatting, install `requirements-dev.txt` and run
`python -m black --check app tests train.py`. CI also checks unit/HTTP tests and
regenerates the model to verify that the committed artifact is reproducible.

## How the two capabilities work

1. **Risk:** eight binary features describe inactive/unknown coverage, missing
   eligibility-check date, denied/pending/unknown authorization, local service
   codes, and absent supporting summary. A trained logistic regression converts
   the feature vector to a score from 0 to 100. Demonstration thresholds are low
   below 30, medium from 30 to below 65, and high from 65. They are not operationally
   validated thresholds. A local code or absent summary is a review prompt, not
   evidence that a payer will deny a claim.
2. **Explanation:** each active feature has an exact coefficient contribution in
   log-odds. Intercept plus all contributions reconstructs the unrounded prediction.
   Findings are sorted by contribution, with recorded evidence and a versioned,
   curated corrective-action catalogue. No language model invents reasons or edits
   clinical codes, eligibility or authorization. Model association is not causation.

The backend extracts only necessary indicators; patient/provider/member references,
diagnosis/procedure identifiers, monetary values and clinical free text are not sent.
The presence of diagnoses and summary is passed as a placeholder. The model does
not assess the clinical meaning of text or validate coding/medical necessity.

## Training and evidence

Run `dev.ps1 train` to reproduce `app/ml/synthetic-model.json`: seed 2718, 4,000
invented examples, 3,200 training and 800 held-out rows. Labels are sampled from an
explicit invented logistic function. Training uses gradient descent and L2
regularization. The JSON records the generator assumptions, learned coefficients,
feature ordering and synthetic test metrics. These metrics measure imitation of
the synthetic generator, **not performance on actual payer outcomes**.

No real outcome dataset is available. A production model needs authorized labeled
claims/outcomes, a defined prediction window and denial target, leakage controls,
patient/provider and time-separated validation, calibration and subgroup checks,
payer-specific evaluation, monitoring and governed model releases.

## API and limits

`POST /api/v1/claims/{id}/assessment`, with the existing bearer token and latest
`If-Match` claim ETag, returns claim ID/version, model version, assessment time,
score, level, evidence, contributions, suggested corrections and limitations.
Existing claim-read scope and ownership checks run first; changes during inference
discard the result with 412. Missing context returns 422; service failure returns
503. There is no fallback score and no automatic approval, rejection or submission.

Assessments are on-demand and are **not yet persisted as an audit history**. The
panel invalidates results when the claim version changes. No duplicate detection,
timely-filing checks, payer-policy retrieval, code compatibility or historical
outcome analysis is implemented. Existing claim lifecycle and validation remain
independent of this advisory feature.
