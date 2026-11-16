import {
  Link,
  Navigate,
  Route,
  Routes,
  useLocation,
  useSearchParams,
  type Location,
} from 'react-router-dom';
import { useEffect } from 'react';
import type { ClaimsGateway } from './model';
import type { Session } from '../auth/session';
import { ClaimsListPage } from './ClaimsListPage';
import { ClaimDetailPage, ClaimEditorDialog } from './ClaimPages';
import { pageNumber } from './routeLinks';
import { WorkspaceInfoPage } from './WorkspaceInfoPage';
// Routes select a page; new/edit routes overlay a dialog on the background page.
export function ClaimsWorkspace({
  api,
  session,
  onDisconnect,
}: {
  api: ClaimsGateway;
  session: Session;
  onDisconnect(): void;
}) {
  const location = useLocation();
  const [search] = useSearchParams();
  const editing = location.pathname.match(/^\/claims\/([^/]+)\/edit$/);
  const popup = location.pathname === '/claims/new' || !!editing;
  const page = pageNumber(search.get('returnPage'));
  const background = (
    location.state as { backgroundLocation?: Location } | null
  )?.backgroundLocation;
  const fallback = {
    pathname: editing ? `/claims/${editing[1]}` : '/claims',
    search: editing ? `?returnPage=${page}` : `?page=${page}`,
  };
  useEffect(() => {
    if (popup) return;
    requestAnimationFrame(() => {
      window.scrollTo(0, 0);
      const title = document.querySelector<HTMLElement>('main h1');
      title?.setAttribute('tabindex', '-1');
      title?.focus({ preventScroll: true });
    });
  }, [location.pathname, popup]);
  return (
    <>
      <div inert={popup ? true : undefined}>
        <Routes location={popup ? (background ?? fallback) : location}>
          <Route path="/" element={<Navigate to="/claims" replace />} />
          <Route
            path="/claims"
            element={<ClaimsListPage api={api} scopes={session.scopes} />}
          />
          <Route
            path="/claims/:id"
            element={<ClaimDetailPage api={api} scopes={session.scopes} />}
          />
          <Route
            path="/workflow"
            element={
              <WorkspaceInfoPage
                type="workflow"
                session={session}
                onDisconnect={onDisconnect}
              />
            }
          />
          <Route
            path="/session"
            element={
              <WorkspaceInfoPage
                type="session"
                session={session}
                onDisconnect={onDisconnect}
              />
            }
          />
          <Route
            path="*"
            element={
              <main id="main-content" className="workspace page-state">
                <h1>Page not found</h1>
                <Link to="/claims">Back to claims</Link>
              </main>
            }
          />
        </Routes>
      </div>
      {popup && (
        <ClaimEditorDialog
          key={editing?.[1] ?? 'new'}
          api={api}
          scopes={session.scopes}
          id={editing?.[1]}
        />
      )}
    </>
  );
}
