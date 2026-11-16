import { describe, expect, it, vi } from 'vitest';
import { HttpClaimsGateway } from './claimsApi';
import { ApiError, errorMessage } from './http';
import type { ClaimInput } from '../features/claims/model';
const claim = { id: 'abc', version: 2 };
describe('HTTP claims adapter', () => {
  it('preserves the creation key when retrying after a lost response', async () => {
    const fetcher = vi
      .fn<typeof fetch>()
      .mockRejectedValueOnce(new TypeError('Connection lost'))
      .mockResolvedValueOnce(
        new Response(JSON.stringify(claim), { headers: { ETag: '"2"' } }),
      );
    const api = new HttpClaimsGateway('token', fetcher);
    const input: ClaimInput = {
      patientReference: 'PAT-DEMO',
      providerReference: 'PROV-DEMO',
      amount: 125,
      currency: 'USD',
      serviceDate: '2026-01-01',
      context: {
        insurance: {
          payerName: 'Demo',
          planReference: 'PLAN',
          memberReference: 'MEM',
          coverageStatus: 'UNKNOWN',
          coverageVerifiedOn: null,
          authorizationStatus: 'NOT_REQUIRED',
          authorizationReference: null,
        },
        diagnosisCodes: ['Z00.00'],
        clinicalSummary: null,
        serviceLines: [
          {
            codeSystem: 'LOCAL',
            procedureCode: 'DEMO',
            description: 'Demo',
            units: 1,
            unitPrice: 125,
          },
        ],
      },
    };
    const key = crypto.randomUUID();
    await expect(api.create(input, key)).rejects.toThrow('Connection lost');
    await api.create(input, key);
    for (const [, options] of fetcher.mock.calls) {
      expect(new Headers(options?.headers).get('Idempotency-Key')).toBe(key);
      expect(options?.body).toBe(JSON.stringify(input));
    }
  });
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
