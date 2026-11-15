import { useEffect, useState } from 'react';
import type { ClaimsGateway, VersionedClaim } from './model';
import { errorMessage } from '../../services/http';
export function useClaim(api: ClaimsGateway, id: string, revision = '') {
  const [result, setResult] = useState<{
    api: ClaimsGateway;
    id: string;
    revision: string;
    data: VersionedClaim | null;
    error: string;
  } | null>(null);
  useEffect(() => {
    let active = true;
    const controller = new AbortController();
    api
      .get(id, controller.signal)
      .then((data) => {
        if (active) setResult({ api, id, revision, data, error: '' });
      })
      .catch((error) => {
        if (active)
          setResult({
            api,
            id,
            revision,
            data: null,
            error: errorMessage(error),
          });
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [api, id, revision]);
  const current =
    result?.api === api && result.id === id && result.revision === revision
      ? result
      : null;
  return {
    data: current?.data ?? null,
    error: current?.error ?? '',
    loading: current === null,
  };
}
