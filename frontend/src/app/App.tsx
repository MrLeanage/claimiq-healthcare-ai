import { useEffect, useMemo, useState } from 'react';
import type { Session } from '../features/auth/session';
import { Connect } from '../features/auth/Connect';
import { ClaimsWorkspace } from '../features/claims/ClaimsWorkspace';
import { HttpClaimsGateway } from '../services/claimsApi';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useDraftGuard } from './draftGuardContext';
export function App() {
  const navigate = useNavigate();
  const location = useLocation();
  const guard = useDraftGuard();
  const markDirty = guard.markDirty;
  const [session, setSession] = useState<Session | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const modalOpen =
    !!session &&
    (location.pathname === '/claims/new' ||
      /^\/claims\/[^/]+\/edit$/.test(location.pathname));
  function disconnect() {
    guard.confirmLeave(() => {
      markDirty(false);
      setSession(null);
      navigate('/claims', { replace: true });
    });
  }
  const [expired, setExpired] = useState(false);
  const api = useMemo(
    () => (session ? new HttpClaimsGateway(session.token) : null),
    [session],
  );
  useEffect(() => {
    if (!session) return;
    const check = () => {
      if (Date.now() >= session.expiresAt) {
        markDirty(false);
        setSession(null);
        setExpired(true);
      }
    };
    check();
    const timer = window.setInterval(check, 1000);
    return () => window.clearInterval(timer);
  }, [session, markDirty]);
  return (
    <>
      <a className="skip-link" href="#main-content">
        Skip to content
      </a>
      <header className="app-header" inert={modalOpen ? true : undefined}>
        <Link className="brand" to="/claims" aria-label="ClaimIQ home">
          <span className="brand-mark" aria-hidden="true">
            ＋
          </span>
          Claim<span>IQ</span>
        </Link>
        {session && (
          <button
            className="button secondary compact menu-toggle"
            aria-controls="workspace-menu"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((value) => !value)}
          >
            Menu
          </button>
        )}
        <span className="header-divider" />
        <span className="header-label">Claims management</span>
        <div className="header-right">
          {session ? (
            <>
              <div className="session-info">
                <span className="session-dot" />
                <span>{session.subject}</span>
                <small>
                  {session.scopes.includes('claims:review')
                    ? 'Reviewer'
                    : session.scopes.includes('claims:write')
                      ? 'Claim writer'
                      : 'Read only'}
                </small>
              </div>
              <button className="button secondary compact" onClick={disconnect}>
                Disconnect
              </button>
            </>
          ) : (
            <span className="environment-label">Secure workspace</span>
          )}
        </div>
      </header>
      <div className={session ? 'application-shell' : 'connection-shell'}>
        {session && (
          <aside
            id="workspace-menu"
            className={`app-sidebar ${menuOpen ? 'is-open' : ''}`}
            inert={modalOpen ? true : undefined}
          >
            <span className="sidebar-caption">WORKSPACE</span>
            <nav aria-label="Main menu">
              <NavLink to="/claims" onClick={() => setMenuOpen(false)}>
                Claims
              </NavLink>
              <NavLink to="/workflow" onClick={() => setMenuOpen(false)}>
                Workflow guide
              </NavLink>
              <NavLink to="/session" onClick={() => setMenuOpen(false)}>
                Your session
              </NavLink>
            </nav>
            <p className="sidebar-note">
              ClaimIQ
              <br />
              <span>Healthcare claims management</span>
            </p>
          </aside>
        )}
        <div className="application-content">
          {session && api ? (
            <ClaimsWorkspace
              key={session.token}
              api={api}
              session={session}
              onDisconnect={disconnect}
            />
          ) : (
            <>
              {expired && (
                <div role="alert" className="notice error session-alert">
                  Your session expired. Connect with a fresh token.
                </div>
              )}
              <Connect
                onConnect={(value) => {
                  setExpired(false);
                  setSession(value);
                  if (location.pathname === '/')
                    navigate('/claims', { replace: true });
                }}
              />
            </>
          )}
        </div>
      </div>
      <footer className="app-footer" inert={modalOpen ? true : undefined}>
        <span>
          ClaimIQ{' '}
          <span className="muted">
            / Healthcare claims, thoughtfully managed.
          </span>
        </span>
        <span>Demo workspace · Synthetic data only</span>
      </footer>
    </>
  );
}
