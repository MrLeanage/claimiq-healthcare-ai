export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401)
      return 'Your session has expired or is invalid. Disconnect and connect with a fresh token.';
    if (error.status === 403)
      return 'Your account does not have permission for this action.';
    if (error.status === 404)
      return 'This claim is unavailable or belongs to another account.';
    if (error.status === 412)
      return 'This claim changed in another session. Reload the claim before trying again. Your changes have not been saved.';
    return error.message;
  }
  return 'Cannot reach ClaimIQ. Check that the backend is running, then try again.';
}
export type Fetcher = typeof fetch;
export async function request(
  fetcher: Fetcher,
  token: string,
  path: string,
  init: RequestInit = {},
): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set('Authorization', `Bearer ${token}`);
  if (init.body) headers.set('Content-Type', 'application/json');
  const response = await fetcher(path, {
    ...init,
    headers,
    credentials: 'omit',
    cache: 'no-store',
  });
  if (!response.ok) {
    let message = `Request failed (${response.status}).`;
    try {
      const body: unknown = await response.json();
      if (
        typeof body === 'object' &&
        body !== null &&
        'message' in body &&
        typeof body.message === 'string'
      )
        message = body.message;
    } catch {
      /* Security responses may have an empty body. */
    }
    throw new ApiError(response.status, message);
  }
  return response;
}
