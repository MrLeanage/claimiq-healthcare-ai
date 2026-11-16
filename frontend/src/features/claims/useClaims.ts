import { useCallback, useEffect, useState } from 'react';
import type { ClaimPage, ClaimsGateway } from './model';
import { errorMessage } from '../../services/http';
interface Result {
  api: ClaimsGateway;
  page: number;
  revision: number;
  data: ClaimPage | null;
  error: string;
}
// This hook owns list loading/error state, separate from rendering and HTTP transport.
export function useClaims(api: ClaimsGateway, page: number) {
  const [result, setResult] = useState<Result | null>(null);
  const [revision, setRevision] = useState(0);
  const refresh = useCallback(() => setRevision((value) => value + 1), []);
  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    api
      .list(page, controller.signal)
      .then((data) => {
        if (active) setResult({ api, page, revision, data, error: '' });
      })
      .catch((error) => {
        if (active)
          setResult({
            api,
            page,
            revision,
            data: null,
            error: errorMessage(error),
          });
      });
    // Abort saves work; active also prevents late responses from updating obsolete state.
    return () => {
      active = false;
      controller.abort();
    };
  }, [api, page, revision]);
  const current =
    result?.api === api && result.page === page && result.revision === revision
      ? result
      : null;
  return {
    data: current?.data ?? null,
    loading: current === null,
    error: current?.error ?? '',
    refresh,
  };
}
