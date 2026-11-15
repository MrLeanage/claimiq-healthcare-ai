import { useState, type FormEvent } from 'react';
import { parseSession, type Session } from './session';
import { HttpClaimsGateway } from '../../services/claimsApi';
import { errorMessage } from '../../services/http';
export function Connect({ onConnect }: { onConnect(session: Session): void }) {
  const [token, setToken] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      const session = parseSession(token);
      await new HttpClaimsGateway(session.token).list(0);
      setToken('');
      onConnect(session);
    } catch (error) {
      setError(
        error instanceof Error && error.message.startsWith('Enter a complete')
          ? error.message
          : errorMessage(error),
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <main id="main-content" className="connect-layout">
      <section className="connect-story">
        <div className="eyebrow">A clearer path from claim to decision</div>
        <h1>
          Less friction.
          <br />
          <span>More clarity.</span>
        </h1>
        <p>
          One focused workspace to prepare claims, coordinate reviews, and move
          decisions forward.
        </p>
        <div className="story-steps">
          <span>01 · Prepare</span>
          <span>02 · Submit</span>
          <span>03 · Review</span>
        </div>
        <div className="story-note">
          Built around a deliberate, traceable claims workflow.
        </div>
      </section>
      <section className="connect-card">
        <span className="eyebrow">Welcome to ClaimIQ</span>
        <h2>Connect to your workspace</h2>
        <p className="muted">
          Use a JWT access token from your provider or the backend demo script.
        </p>
        <form onSubmit={(e) => void submit(e)}>
          <label>
            Access token
            <textarea
              required
              rows={5}
              autoComplete="off"
              spellCheck={false}
              value={token}
              onChange={(e) => setToken(e.target.value)}
              placeholder="Paste your access token"
              disabled={busy}
            />
          </label>
          {error && (
            <div role="alert" className="notice error">
              {error}
            </div>
          )}
          <button className="button primary wide" disabled={busy}>
            {busy ? 'Connecting…' : 'Connect workspace →'}
          </button>
        </form>
        <div className="privacy-note">
          <strong>Only for this session</strong>
          <p>
            Your token stays in browser memory. Refreshing or disconnecting
            clears it.
          </p>
        </div>
        <p className="small muted">
          Demo: run <code>.\dev.ps1 demo</code> inside backend, then copy the
          writer or reviewer token.
        </p>
      </section>
    </main>
  );
}
