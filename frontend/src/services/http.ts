// A standard JavaScript Error with an HTTP status for UI-specific error handling.
export class ApiError extends Error {
  constructor(
    // Parameter property: declare and initialize status; readonly is a TypeScript check.
    public readonly status: number,
    message: string,
  ) {
    // The parent Error constructor supplies the inherited message property.
    super(message);
    this.name = 'ApiError';
  }
}
// unknown must be narrowed before reading properties; this maps errors to user feedback.
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
// A callable type, not a server: production uses browser fetch; tests inject a substitute.
export type Fetcher = typeof fetch;
// async returns a Promise<Response>. init defaults to an empty options object.
export async function request(
  fetcher: Fetcher,
  token: string,
  path: string,
  init: RequestInit = {},
): Promise<Response> {
  // const protects the variable binding, not the mutable Headers object. Preserve caller headers.
  const headers = new Headers(init.headers);
  headers.set('Authorization', `Bearer ${token}`);
  if (init.body) headers.set('Content-Type', 'application/json');
  // await pauses this function, not the browser thread; the body is read separately.
  const response = await fetcher(path, {
    // Shallow-copy method/body/signal; later properties override matching copied options.
    ...init,
    headers,
    // Omit browser-managed credentials (cookies), not our explicit bearer header.
    credentials: 'omit',
    // Bypass the HTTP cache and do not store this response there; React may still hold UI state.
    cache: 'no-store',
  });
  // fetch resolves for HTTP errors too; ok means 200-299, not just 200.
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
    // Throwing here rejects the promise. Network failures can reject before any response exists.
    throw new ApiError(response.status, message);
  }
  // Success leaves JSON parsing to the caller, so the body is not consumed twice.
  return response;
}
