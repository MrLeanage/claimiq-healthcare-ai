import { useEffect, useRef, useState } from 'react';
import {
  Link,
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from 'react-router-dom';
import { useDraftGuard } from '../../app/draftGuardContext';
import { ClaimForm } from './ClaimForm';
import { ClaimDetails } from './ClaimDetails';
import { useClaim } from './useClaim';
import { claimPath, listPath, pageNumber } from './routeLinks';
import type { ClaimInput, ClaimsGateway, VersionedClaim } from './model';
import { ApiError, errorMessage } from '../../services/http';
function LoadState({ error, loading }: { error: string; loading: boolean }) {
  return (
    <section className="panel page-state">
      {loading ? (
        <p role="status">Loading claim...</p>
      ) : (
        <div role="alert" className="notice error">
          {error}
        </div>
      )}
    </section>
  );
}
function PageHeading({
  title,
  page,
  subtitle,
}: {
  title: string;
  page: number;
  subtitle: string;
}) {
  return (
    <div className="page-heading">
      <div>
        <div className="breadcrumb">
          <Link to={listPath(page)}>Claims</Link>
          <span>/</span>
          {title}
        </div>
        <h1 tabIndex={-1}>{title}</h1>
        <p className="muted">{subtitle}</p>
      </div>
      <Link className="button secondary" to={listPath(page)}>
        Back to claims
      </Link>
    </div>
  );
}
export function ClaimDetailPage({
  api,
  scopes,
}: {
  api: ClaimsGateway;
  scopes: readonly string[];
}) {
  const { id = '' } = useParams();
  const [search] = useSearchParams();
  const page = pageNumber(search.get('returnPage'));
  const navigate = useNavigate();
  const location = useLocation();
  const { data, error, loading } = useClaim(api, id, location.key);
  const notice =
    typeof location.state?.notice === 'string' ? location.state.notice : '';
  return (
    <main id="main-content" className="workspace focused-page">
      <PageHeading
        title="Claim details"
        page={page}
        subtitle="Review one claim and its recorded information."
      />
      {!data ? (
        <LoadState error={error} loading={loading} />
      ) : (
        <ClaimDetails
          key={data.claim.id + data.etag}
          initial={data}
          api={api}
          canWrite={scopes.includes('claims:write')}
          canReview={scopes.includes('claims:review')}
          initialNotice={notice}
          onEdit={() =>
            navigate(
              `/claims/${encodeURIComponent(id)}/edit?returnPage=${page}`,
              { state: { backgroundLocation: location } },
            )
          }
        />
      )}
    </main>
  );
}
function Editor({
  api,
  initial,
  page,
}: {
  api: ClaimsGateway;
  initial?: VersionedClaim;
  page: number;
}) {
  const navigate = useNavigate();
  const guard = useDraftGuard();
  const active = useRef(true);
  // One key per open editor survives retries, but not a refresh or new form.
  const [creationKey] = useState(() => crypto.randomUUID());
  const markSaving = guard.markSaving;
  useEffect(() => {
    active.current = true;
    return () => {
      active.current = false;
      markSaving(false);
    };
  }, [markSaving]);
  const [busy, setBusy] = useState(false);
  const [stale, setStale] = useState(false);
  const [error, setError] = useState('');
  // The form provides validated input; navigate only after the API confirms saving.
  async function save(input: ClaimInput) {
    setBusy(true);
    guard.markSaving(true);
    setError('');
    try {
      const saved = initial
        ? await api.update(initial.claim.id, input, initial.etag)
        : await api.create(input, creationKey);
      if (!active.current) return;
      guard.markDirty(false);
      guard.markSaving(false);
      navigate(claimPath(saved.claim.id, initial ? page : 0), {
        replace: true,
        state: { notice: initial ? 'Draft updated.' : 'Draft created.' },
      });
    } catch (error) {
      if (active.current) {
        setError(errorMessage(error));
        if (error instanceof ApiError && error.status === 412) setStale(true);
      }
    } finally {
      if (active.current) {
        setBusy(false);
        guard.markSaving(false);
      }
    }
  }
  return (
    <section className="editor-content">
      {error && (
        <div role="alert" className="notice error">
          {error}
        </div>
      )}
      {stale && initial && (
        <button
          className="button secondary reload-editor"
          disabled={busy}
          onClick={() =>
            guard.confirmLeave(() => {
              guard.markDirty(false);
              navigate(claimPath(initial.claim.id, page), { replace: true });
            })
          }
        >
          Reload latest claim
        </button>
      )}
      <ClaimForm
        initial={initial?.claim}
        busy={busy}
        locked={stale}
        onSave={save}
        onDirtyChange={guard.markDirty}
        onCancel={() =>
          navigate(initial ? claimPath(initial.claim.id, page) : listPath(page))
        }
      />
    </section>
  );
}
export function ClaimEditorDialog({
  api,
  scopes,
  id,
}: {
  api: ClaimsGateway;
  scopes: readonly string[];
  id?: string;
}) {
  const [search] = useSearchParams();
  const page = pageNumber(search.get('returnPage'));
  const navigate = useNavigate();
  const close = useRef<HTMLButtonElement>(null);
  const dialog = useRef<HTMLElement>(null);
  const goBack = () =>
    navigate(id ? claimPath(id, page) : listPath(page), { replace: true });
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    close.current?.focus();
    return () => {
      document.body.style.overflow = overflow;
      requestAnimationFrame(() => {
        if (previous?.isConnected) previous.focus();
      });
    };
  }, []);
  return (
    <div className="editor-overlay">
      <section
        ref={dialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby="editor-dialog-title"
        className="claim-dialog"
        onKeyDown={(event) => {
          if (event.key === 'Escape') {
            event.preventDefault();
            goBack();
          }
          if (event.key === 'Tab') {
            const targets = [
              ...dialog.current!.querySelectorAll<HTMLElement>(
                'button:not(:disabled),input:not(:disabled),select:not(:disabled),textarea:not(:disabled),a[href],[tabindex="0"]',
              ),
            ].filter(
              (el) => el.getClientRects().length > 0 && !el.closest('[inert]'),
            );
            const first = targets[0],
              last = targets[targets.length - 1];
            if (event.shiftKey && document.activeElement === first) {
              event.preventDefault();
              last?.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
              event.preventDefault();
              first?.focus();
            }
          }
        }}
      >
        <div className="dialog-heading">
          <div>
            <span className="eyebrow">Claim information</span>
            <h1 id="editor-dialog-title">{id ? 'Edit draft' : 'New claim'}</h1>
            <p className="small muted">
              Choose a section to view or edit. You can save from any section.
            </p>
          </div>
          <button
            ref={close}
            className="icon-button"
            aria-label="Close claim form"
            onClick={goBack}
          >
            ×
          </button>
        </div>
        {!scopes.includes('claims:write') ? (
          <div role="alert" className="notice error">
            You do not have permission to create or edit claims.
          </div>
        ) : id ? (
          <ExistingEditor api={api} id={id} page={page} />
        ) : (
          <Editor api={api} page={page} />
        )}
      </section>
    </div>
  );
}
function ExistingEditor({
  api,
  id,
  page,
}: {
  api: ClaimsGateway;
  id: string;
  page: number;
}) {
  const { data, error, loading } = useClaim(api, id);
  if (!data) return <LoadState error={error} loading={loading} />;
  if (data.claim.status !== 'DRAFT')
    return (
      <div role="alert" className="notice error">
        Only draft claims can be edited.{' '}
        <Link to={claimPath(id, page)}>View this claim</Link>
      </div>
    );
  return <Editor key={id} api={api} initial={data} page={page} />;
}
