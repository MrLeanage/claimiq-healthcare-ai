import { useEffect, useRef, useState } from 'react';
import type { ClaimsGateway, VersionedClaim } from './model';
import type { Assessment } from './assessment';
import { errorMessage } from '../../services/http';

export function ClaimAssessment({
  api,
  current,
  disabled,
}: {
  api: ClaimsGateway;
  current: VersionedClaim;
  disabled: boolean;
}) {
  const [result, setResult] = useState<Assessment | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const pending = useRef<AbortController | null>(null);
  useEffect(
    () => () => pending.current?.abort(),
    [current.claim.id, current.claim.version],
  );
  const valid =
    result?.claimId === current.claim.id &&
    result.claimVersion === current.claim.version;
  async function run() {
    if (!api.assess) return;
    pending.current?.abort();
    const controller = new AbortController();
    pending.current = controller;
    setBusy(true);
    setError('');
    setResult(null);
    try {
      const response = await api.assess(
        current.claim.id,
        current.etag,
        controller.signal,
      );
      if (!controller.signal.aborted) setResult(response);
    } catch (failure) {
      if (!controller.signal.aborted) setError(errorMessage(failure));
    } finally {
      if (pending.current === controller) setBusy(false);
    }
  }
  if (!api.assess) return null;
  return (
    <section aria-labelledby="assessment-title" className="claim-section">
      <h3 id="assessment-title">Pre-submission risk assessment</h3>
      <p className="notice">
        Synthetic demonstration. Scores are not validated payer denial
        probabilities. Human review is required.
      </p>
      <button
        className="button secondary"
        disabled={disabled || busy || !current.claim.context}
        onClick={() => void run()}
      >
        {busy ? 'Assessing claim…' : 'Assess denial risk'}
      </button>
      {error && (
        <p role="alert" className="notice error">
          {error}
        </p>
      )}
      {result && !valid && (
        <p role="status">The claim changed. Run a new assessment.</p>
      )}
      {valid && result && (
        <div role="status">
          <h4>
            {result.assessment.riskLevel} risk ·{' '}
            {result.assessment.score.toFixed(1)} / 100
          </h4>
          <p className="small muted">
            Model {result.assessment.modelVersion} · Claim version{' '}
            {result.claimVersion} ·{' '}
            {new Date(result.assessment.assessedAt).toLocaleString()}
          </p>
          {result.assessment.findings.length === 0 && (
            <p>
              No modeled risk factors found. This does not establish that the
              claim will be accepted.
            </p>
          )}
          {result.assessment.findings.map((finding) => (
            <article key={finding.code} className="notice">
              <strong>{finding.title}</strong>
              <p>Recorded evidence: {finding.evidence}</p>
              <p>{finding.suggestedCorrection}</p>
              <details>
                <summary>Model contribution</summary>
                <p>
                  {finding.contributionLogOdds.toFixed(3)} log-odds from{' '}
                  {finding.field}. This is a model contribution, not a
                  percentage or proof of causation.
                </p>
              </details>
            </article>
          ))}
          <details>
            <summary>Assessment limits</summary>
            <ul>
              {result.assessment.limitations.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </details>
        </div>
      )}
    </section>
  );
}
