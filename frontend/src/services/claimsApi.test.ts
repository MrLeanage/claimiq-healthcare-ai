import { describe, expect, it, vi } from 'vitest';
import { HttpClaimsGateway } from './claimsApi';
import { ApiError, errorMessage } from './http';
const claim = { id: 'abc', version: 2 };
describe('HTTP claims adapter', () => {
  it('preserves the server ETag and sends it on the next mutation', async () => {
    const fetcher = vi
      .fn<typeof fetch>()
      .mockImplementation(
        async () =>
          new Response(JSON.stringify(claim), { headers: { ETag: '"2"' } }),
      );
    const api = new HttpClaimsGateway('access-token', fetcher);
    const current = await api.get('abc');
    await api.submit('abc', current.etag);
    const [url, options] = fetcher.mock.calls[1];
    expect(url).toBe('/api/v1/claims/abc/submit');
    expect(new Headers(options?.headers).get('If-Match')).toBe('"2"');
    expect(new Headers(options?.headers).get('Authorization')).toBe(
      'Bearer access-token',
    );
    expect(options?.credentials).toBe('omit');
  });
  it('does not silently retry a stale write', async () => {
    const fetcher = vi
      .fn<typeof fetch>()
      .mockResolvedValue(new Response('', { status: 412 }));
    await expect(
      new HttpClaimsGateway('token', fetcher).submit('abc', '"0"'),
    ).rejects.toMatchObject({ status: 412 });
    expect(fetcher).toHaveBeenCalledTimes(1);
    expect(errorMessage(new ApiError(412, ''))).toContain('Reload');
  });
  it('handles empty authentication error responses', async () => {
    const fetcher = vi
      .fn<typeof fetch>()
      .mockResolvedValue(new Response(null, { status: 401 }));
    await expect(
      new HttpClaimsGateway('token', fetcher).list(0),
    ).rejects.toMatchObject({ status: 401 });
  });
  it('rejects missing version metadata instead of guessing it', async () => {
    const fetcher = vi
      .fn<typeof fetch>()
      .mockResolvedValue(new Response(JSON.stringify(claim)));
    await expect(
      new HttpClaimsGateway('token', fetcher).get('abc'),
    ).rejects.toThrow('ETag');
  });
});
