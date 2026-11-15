export interface Session {
  readonly token: string;
  readonly subject: string;
  readonly scopes: readonly string[];
  readonly expiresAt: number;
}
// Decoded claims are display hints only. The backend verifies the signature and permissions.
export function parseSession(token: string): Session {
  try {
    const parts = token.trim().split('.');
    if (parts.length !== 3) throw new Error();
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const data: unknown = JSON.parse(
      new TextDecoder().decode(
        Uint8Array.from(
          atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')),
          (c) => c.charCodeAt(0),
        ),
      ),
    );
    if (
      !data ||
      typeof data !== 'object' ||
      !('sub' in data) ||
      typeof data.sub !== 'string' ||
      !data.sub.trim() ||
      !('exp' in data) ||
      typeof data.exp !== 'number' ||
      !Number.isFinite(data.exp) ||
      data.exp * 1000 <= Date.now()
    )
      throw new Error();
    const scope = 'scope' in data ? data.scope : 'scp' in data ? data.scp : '';
    const scopes =
      typeof scope === 'string'
        ? scope.split(/\s+/)
        : Array.isArray(scope)
          ? scope.filter((s): s is string => typeof s === 'string')
          : [];
    return {
      token: token.trim(),
      subject: data.sub,
      scopes,
      expiresAt: data.exp * 1000,
    };
  } catch {
    throw new Error('Enter a complete, unexpired JWT access token.');
  }
}
