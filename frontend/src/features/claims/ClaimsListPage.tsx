import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import type { ClaimsGateway } from './model';
import { formatAmount } from './model';
import { useClaims } from './useClaims';
import { StatusBadge } from '../../components/StatusBadge';
import { claimPath, pageNumber } from './routeLinks';
interface Props {
  api: ClaimsGateway;
  scopes: readonly string[];
}
export function ClaimsListPage({ api, scopes }: Props) {
  const navigate = useNavigate();
  const location = useLocation();
  const [search, setSearch] = useSearchParams();
  const page = pageNumber(search.get('page'));
  const { data, loading, error, refresh } = useClaims(api, page);
  function setPage(value: number) {
    setSearch({ page: String(value) });
  }
  function open(id: string) {
    navigate(claimPath(id, page));
  }
  const canWrite = scopes.includes('claims:write');
  const canReview = scopes.includes('claims:review');
  const claims = data?.content ?? [];
  return (
    <main id="main-content" className="workspace">
      <div className="page-heading">
        <div>
          <div className="breadcrumb">
            Workspace <span>/</span> Claims
          </div>
          <h1>
            Claims workspace<span className="title-dot">.</span>
          </h1>
          <p className="muted">
            {canReview
              ? 'Review submissions and move claims toward a clear decision.'
              : 'Prepare, submit, and follow your claims in one place.'}
          </p>
        </div>
        {canWrite && (
          <button
            className="button primary"
            disabled={loading}
            onClick={() => {
              navigate(`/claims/new?returnPage=${page}`, {
                state: { backgroundLocation: location },
              });
            }}
          >
            ＋ New claim
          </button>
        )}
      </div>
      <div className="summary-grid">
        <div className="summary-card">
          <span>Total visible claims</span>
          <strong>{loading ? '—' : (data?.totalElements ?? '—')}</strong>
          <small>Within your account permissions</small>
        </div>
        <div className="summary-card">
          <span>Ready to prepare</span>
          <strong>
            {loading ? '—' : claims.filter((c) => c.status === 'DRAFT').length}
          </strong>
          <small>Drafts on this page</small>
        </div>
        <div className="summary-card">
          <span>In the review journey</span>
          <strong>
            {loading
              ? '—'
              : claims.filter(
                  (c) =>
                    c.status === 'SUBMITTED' || c.status === 'UNDER_REVIEW',
                ).length}
          </strong>
          <small>Submitted or under review on this page</small>
        </div>
        <div className="summary-card accent">
          <span>A considered workflow</span>
          <strong>Every step matters</strong>
          <small>Prepare → Submit → Review → Decide</small>
        </div>
      </div>
      <div className="workspace-grid">
        <section
          className="panel claims-panel"
          aria-labelledby="claims-heading"
        >
          <div className="panel-heading">
            <div>
              <h2 id="claims-heading">
                {canReview ? 'Claims to oversee' : 'Your claims'}
              </h2>
              <p className="small muted">Latest claims first · 10 per page</p>
            </div>
            <button
              className="button secondary compact"
              disabled={loading}
              onClick={refresh}
            >
              ↻ Refresh
            </button>
          </div>
          {error && (
            <div role="alert" className="notice error">
              {error}
              <button className="button text" onClick={refresh}>
                Try again
              </button>
            </div>
          )}
          {loading ? (
            <div className="empty-state" role="status">
              <span className="spinner" />
              Loading claims…
            </div>
          ) : !error && claims.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon" aria-hidden="true">
                ▤
              </div>
              <h3>No claims here yet</h3>
              <p>
                {canWrite
                  ? 'Create a draft to start your first claim.'
                  : 'Submitted claims will appear here when a writer creates them.'}
              </p>
              {canWrite && (
                <button
                  className="button secondary"
                  onClick={() =>
                    navigate(`/claims/new?returnPage=${page}`, {
                      state: { backgroundLocation: location },
                    })
                  }
                >
                  Create your first claim
                </button>
              )}
            </div>
          ) : (
            !error && (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Claim / patient</th>
                      <th>Provider</th>
                      <th>Amount</th>
                      <th>Status</th>
                      <th>
                        <span className="sr-only">Action</span>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {claims.map((claim) => (
                      <tr key={claim.id}>
                        <td>
                          <button
                            className="claim-link"
                            disabled={loading}
                            onClick={() => void open(claim.id)}
                          >
                            {claim.claimNumber}
                          </button>
                          <span className="cell-subtitle">
                            {claim.patientReference}
                          </span>
                        </td>
                        <td>
                          {claim.providerReference}
                          <span className="cell-subtitle">
                            {claim.serviceDate}
                          </span>
                        </td>
                        <td className="money">
                          {formatAmount(claim.amount, claim.currency)}
                        </td>
                        <td>
                          <StatusBadge status={claim.status} />
                        </td>
                        <td>
                          <button
                            className="icon-button"
                            disabled={loading}
                            aria-label={`Open ${claim.claimNumber}`}
                            onClick={() => void open(claim.id)}
                          >
                            ↗
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          )}
          <div className="table-footer">
            <span>
              {data && data.totalElements > 0
                ? `${page * 10 + 1}–${Math.min((page + 1) * 10, data.totalElements)} of ${data.totalElements} claims`
                : 'No claims to display'}
            </span>
            <div className="button-row">
              <button
                className="button secondary compact"
                disabled={page === 0 || loading}
                onClick={() => {
                  setPage(page - 1);
                }}
              >
                Previous
              </button>
              <button
                className="button secondary compact"
                disabled={!data || page + 1 >= data.totalPages || loading}
                onClick={() => {
                  setPage(page + 1);
                }}
              >
                Next
              </button>
            </div>
          </div>
        </section>
      </div>
      <div className="workspace-note">
        <span aria-hidden="true">◇</span>
        <p>
          Designed for a clear handoff. Drafts stay editable until submission;
          every status change is recorded by the backend.
        </p>
      </div>
    </main>
  );
}
