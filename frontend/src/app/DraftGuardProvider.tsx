import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { useBeforeUnload, useBlocker } from 'react-router-dom';
import { DraftGuardContext } from './draftGuardContext';
function LeaveDialog({
  saving,
  onStay,
  onDiscard,
}: {
  saving: boolean;
  onStay(): void;
  onDiscard(): void;
}) {
  const stay = useRef<HTMLButtonElement>(null);
  const discard = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    stay.current?.focus();
  }, []);
  return (
    <div className="modal-backdrop">
      <section
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="leave-title"
        aria-describedby="leave-description"
        className="leave-dialog"
        onKeyDown={(event) => {
          if (event.key === 'Escape') {
            event.preventDefault();
            onStay();
          }
          if (event.key === 'Tab') {
            const last = saving ? stay.current : discard.current;
            if (event.shiftKey && document.activeElement === stay.current) {
              event.preventDefault();
              last?.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
              event.preventDefault();
              stay.current?.focus();
            }
          }
        }}
      >
        <h2 id="leave-title">
          {saving ? 'Save in progress' : 'Discard unsaved changes?'}
        </h2>
        <p id="leave-description">
          {saving
            ? 'Please wait while your claim is saved.'
            : 'Your changes have not been saved. Stay here to keep editing, or discard them and leave.'}
        </p>
        <div className="button-row">
          <button ref={stay} className="button primary" onClick={onStay}>
            Stay on this page
          </button>
          <button
            ref={discard}
            className="button secondary"
            disabled={saving}
            onClick={onDiscard}
          >
            Discard and leave
          </button>
        </div>
      </section>
    </div>
  );
}
export function DraftGuardProvider({ children }: { children: ReactNode }) {
  const dirty = useRef(false);
  const saving = useRef(false);
  const [isSaving, setIsSaving] = useState(false);
  const [pending, setPending] = useState<(() => void) | null>(null);
  const markDirty = useCallback((value: boolean) => {
    dirty.current = value;
  }, []);
  const markSaving = useCallback((value: boolean) => {
    saving.current = value;
    setIsSaving(value);
  }, []);
  const confirmLeave = useCallback((action: () => void) => {
    if (dirty.current || saving.current) setPending(() => action);
    else action();
  }, []);
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      (dirty.current || saving.current) &&
      (currentLocation.pathname !== nextLocation.pathname ||
        currentLocation.search !== nextLocation.search),
  );
  useBeforeUnload(
    useCallback((event: BeforeUnloadEvent) => {
      if (dirty.current || saving.current) {
        event.preventDefault();
        event.returnValue = '';
      }
    }, []),
  );
  const blocked = blocker.state === 'blocked' || pending !== null;
  function stay() {
    setPending(null);
    if (blocker.state === 'blocked') blocker.reset();
  }
  function discard() {
    if (saving.current) return;
    markDirty(false);
    if (pending) {
      const action = pending;
      setPending(null);
      action();
    } else if (blocker.state === 'blocked') blocker.proceed();
  }
  return (
    <DraftGuardContext.Provider value={{ markDirty, markSaving, confirmLeave }}>
      <div inert={blocked ? true : undefined}>{children}</div>
      {blocked && (
        <LeaveDialog saving={isSaving} onStay={stay} onDiscard={discard} />
      )}
    </DraftGuardContext.Provider>
  );
}
