import { Link } from 'react-router-dom';
import type { Session } from '../auth/session';
export function WorkspaceInfoPage({
  type,
  session,
  onDisconnect,
}: {
  type: 'workflow' | 'session';
  session: Session;
  onDisconnect(): void;
}) {
  return (
    <main id="main-content" className="workspace focused-page">
      <div className="page-heading">
        <div>
          <div className="breadcrumb">
            <Link to="/claims">Workspace</Link>
            <span>/</span>
            {type === 'workflow' ? 'Workflow guide' : 'Session'}
          </div>
          <h1>{type === 'workflow' ? 'Claim workflow' : 'Your session'}</h1>
        </div>
      </div>
      <section className="panel info-panel">
        {type === 'workflow' ? (
          <>
            <h2>From draft to decision</h2>
            <ol className="workflow-guide">
              <li>
                <strong>Prepare a draft</strong>
                <p>
                  Use New claim in the claims list. Enter basics, insurance,
                  clinical context, and billed services in the popup. Sections
                  can be completed in any order.
                </p>
              </li>
              <li>
                <strong>Submit when ready</strong>
                <p>
                  Open Claim details and submit a complete draft. Submission
                  locks editing.
                </p>
              </li>
              <li>
                <strong>Review</strong>
                <p>
                  A reviewer starts review, examines the recorded information,
                  and approves or rejects the claim.
                </p>
              </li>
            </ol>
            <p className="muted">
              Code and eligibility checks are user-reported. AI assessment is
              not integrated yet.
            </p>
            <Link className="button primary" to="/claims">
              Go to claims
            </Link>
          </>
        ) : (
          <>
            <h2>Connection and permissions</h2>
            <dl className="details-grid">
              <div>
                <dt>Account subject</dt>
                <dd>{session.subject}</dd>
              </div>
              <div>
                <dt>Token expires</dt>
                <dd>{new Date(session.expiresAt).toLocaleString()}</dd>
              </div>
              <div>
                <dt>Granted scopes</dt>
                <dd>{session.scopes.join(', ') || 'None'}</dd>
              </div>
            </dl>
            <p className="muted">
              Tokens stay in browser memory. The backend verifies access on
              every request. Use a fresh token after a backend restart.
            </p>
            <button className="button secondary" onClick={onDisconnect}>
              Disconnect workspace
            </button>
          </>
        )}
      </section>
    </main>
  );
}
