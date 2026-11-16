import { useState } from 'react';
import { ClaimAssessment } from './ClaimAssessment';
import type { ClaimsGateway, ClaimStatus, VersionedClaim } from './model';
import { formatAmount, reviewActions, statusLabels } from './model';
import { Tabs } from '../../components/Tabs';
import { ClaimContextView } from './ClaimContextView';
import { StatusBadge } from '../../components/StatusBadge';
import { ApiError, errorMessage } from '../../services/http';
interface Props {
  initial: VersionedClaim;
  api: ClaimsGateway;
  canWrite: boolean;
  canReview: boolean;
  onEdit(): void;
  initialNotice?: string;
}
export function ClaimDetails({
  initial,
  api,
  canWrite,
  canReview,
  onEdit,
  initialNotice = '',
}: Props) {
  const [current, setCurrent] = useState(initial);
  const [tab, setTab] = useState<
    'overview' | 'insurance' | 'clinical' | 'services'
  >('overview');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [stale, setStale] = useState(false);
  const [confirmation, setConfirmation] = useState<
    ClaimStatus | 'SUBMIT' | null
  >(null);
  const [notice, setNotice] = useState(initialNotice);
  const claim = current.claim;
  // Shared mutation handling retains the returned ETag; 412 requires an explicit reload.
  async function execute(
    operation: () => Promise<VersionedClaim>,
    success: string,
  ) {
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const saved = await operation();
      setCurrent(saved);
      setConfirmation(null);
      setNotice(success);
    } catch (error) {
      setError(errorMessage(error));
      if (error instanceof ApiError && error.status === 412) setStale(true);
    } finally {
      setBusy(false);
    }
  }
  async function reload() {
    setBusy(true);
    setError('');
    try {
      setCurrent(await api.get(claim.id));
      setStale(false);
      setConfirmation(null);
      setNotice('Latest claim loaded.');
    } catch (error) {
      setError(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }
  // UI permissions hide unavailable actions; backend checks enforce actual access.
  const writeEnabled = canWrite && claim.status === 'DRAFT';
  return (
    <section className="panel detail-panel" aria-labelledby="claim-title">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Claim details</span>
          <h2 id="claim-title">{claim.claimNumber}</h2>
        </div>
      </div>
      <div className="detail-topline">
        <StatusBadge status={claim.status} />
        <span className="small muted">Version {claim.version}</span>
      </div>
      {error && (
        <div role="alert" className="notice error">
          {error}
        </div>
      )}
      {notice && (
        <div role="status" className="notice success">
          {notice}
        </div>
      )}
      {stale && (
        <button
          className="button secondary"
          onClick={() => void reload()}
          disabled={busy}
        >
          Reload latest claim
        </button>
      )}
      <Tabs<'overview' | 'insurance' | 'clinical' | 'services'>
        id="claim-information"
        label="Claim information"
        items={
          [
            { value: 'overview', label: 'Overview' },
            { value: 'insurance', label: 'Insurance' },
            { value: 'clinical', label: 'Clinical context' },
            { value: 'services', label: 'Billed services' },
          ] as const
        }
        value={tab}
        onChange={setTab}
      />
      <div
        id="claim-information-panel"
        role="tabpanel"
        aria-labelledby={`claim-information-${tab}-tab`}
        tabIndex={0}
      >
        {!claim.context && tab === 'overview' && (
          <p className="notice context-missing">
            {claim.status === 'DRAFT'
              ? 'Use Edit draft to complete insurance, clinical context, and billed services before submission.'
              : 'This older claim has no recorded assessment information.'}
          </p>
        )}
        {tab === 'overview' ? (
          <>
            <div className="claim-value">
              {formatAmount(claim.amount, claim.currency)}
              <span>Claim amount</span>
            </div>
            <dl className="details-grid">
              <div>
                <dt>Patient reference</dt>
                <dd>{claim.patientReference}</dd>
              </div>
              <div>
                <dt>Provider reference</dt>
                <dd>{claim.providerReference}</dd>
              </div>
              <div>
                <dt>Service date</dt>
                <dd>{claim.serviceDate}</dd>
              </div>
              <div>
                <dt>Created</dt>
                <dd>{new Date(claim.createdAt).toLocaleString()}</dd>
              </div>
              <div>
                <dt>Last updated</dt>
                <dd>{new Date(claim.updatedAt).toLocaleString()}</dd>
              </div>
              <div>
                <dt>Submitted</dt>
                <dd>
                  {claim.submittedAt
                    ? new Date(claim.submittedAt).toLocaleString()
                    : 'Not submitted'}
                </dd>
              </div>
            </dl>

            <div className="lifecycle" aria-label="Claim workflow">
              {['Draft', 'Submitted', 'Review', 'Decision'].map(
                (step, index) => (
                  <span
                    key={step}
                    className={
                      index <=
                      {
                        DRAFT: 0,
                        SUBMITTED: 1,
                        UNDER_REVIEW: 2,
                        APPROVED: 3,
                        REJECTED: 3,
                      }[claim.status]
                        ? 'reached'
                        : ''
                    }
                  >
                    <i>{index + 1}</i>
                    {step}
                  </span>
                ),
              )}
            </div>
          </>
        ) : (
          <ClaimContextView
            context={claim.context}
            currency={claim.currency}
            section={tab}
          />
        )}
      </div>
      <ClaimAssessment
        key={`${claim.id}-${claim.version}`}
        api={api}
        current={current}
        disabled={busy || stale}
      />
      <div className="detail-actions">
        {writeEnabled && (
          <>
            <button
              className="button secondary"
              disabled={busy || stale}
              onClick={onEdit}
            >
              Edit draft
            </button>
            <button
              className="button primary"
              disabled={busy || stale || !claim.context}
              onClick={() => setConfirmation('SUBMIT')}
            >
              Submit claim
            </button>
          </>
        )}
        {canReview &&
          reviewActions(claim.status).map((target) => (
            <button
              key={target}
              className={`button ${target === 'REJECTED' ? 'danger' : 'primary'}`}
              disabled={busy || stale}
              onClick={() => setConfirmation(target)}
            >
              {target === 'UNDER_REVIEW'
                ? 'Start review'
                : target === 'APPROVED'
                  ? 'Approve claim'
                  : 'Reject claim'}
            </button>
          ))}
        <button
          className="button text"
          disabled={busy}
          onClick={() => void reload()}
        >
          Refresh details
        </button>
      </div>
      {confirmation && (
        <div className="confirmation">
          <strong>
            {confirmation === 'SUBMIT'
              ? 'Submit this claim?'
              : `Change status to ${statusLabels[confirmation].toLowerCase()}?`}
          </strong>
          <p className="small muted">
            {confirmation === 'SUBMIT'
              ? 'Submitted claims can no longer be edited.'
              : 'This decision will be recorded in the claim audit trail.'}
          </p>
          <div className="button-row">
            <button
              className="button primary"
              disabled={busy || stale}
              onClick={() =>
                void execute(
                  () =>
                    confirmation === 'SUBMIT'
                      ? api.submit(claim.id, current.etag)
                      : api.transition(claim.id, confirmation, current.etag),
                  'Claim status updated.',
                )
              }
            >
              {busy ? 'Saving…' : 'Confirm'}
            </button>
            <button
              className="button secondary"
              disabled={busy}
              onClick={() => setConfirmation(null)}
            >
              Cancel
            </button>
          </div>
        </div>
      )}
    </section>
  );
}
