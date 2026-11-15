import type {
  ClaimInput,
  ClaimPage,
  ClaimsGateway,
  ClaimStatus,
  VersionedClaim,
} from '../features/claims/model';
import { request, type Fetcher } from './http';
const base = '/api/v1/claims';
export class HttpClaimsGateway implements ClaimsGateway {
  constructor(
    private readonly token: string,
    private readonly fetcher: Fetcher = fetch,
  ) {}
  async list(page: number, signal?: AbortSignal): Promise<ClaimPage> {
    const response = await request(
      this.fetcher,
      this.token,
      `${base}?page=${page}&size=10`,
      { signal },
    );
    return response.json() as Promise<ClaimPage>;
  }
  async assess(id: string, etag: string, signal?: AbortSignal) {
    const response = await request(
      this.fetcher,
      this.token,
      `${base}/${encodeURIComponent(id)}/assessment`,
      { method: 'POST', headers: { 'If-Match': etag }, signal },
    );
    return response.json() as Promise<
      import('../features/claims/assessment').Assessment
    >;
  }
  private async write(
    path: string,
    method: string,
    body?: unknown,
    etag?: string,
  ): Promise<VersionedClaim> {
    return this.readVersioned(
      await request(this.fetcher, this.token, path, {
        method,
        headers: etag ? { 'If-Match': etag } : {},
        body: body === undefined ? undefined : JSON.stringify(body),
      }),
    );
  }
  private async readVersioned(response: Response): Promise<VersionedClaim> {
    const etag = response.headers.get('ETag');
    if (!etag || !/^"\d+"$/.test(etag))
      throw new Error('Backend did not return a valid ETag.');
    return { claim: await response.json(), etag } as VersionedClaim;
  }
  async get(id: string, signal?: AbortSignal): Promise<VersionedClaim> {
    return this.readVersioned(
      await request(
        this.fetcher,
        this.token,
        `${base}/${encodeURIComponent(id)}`,
        { signal },
      ),
    );
  }
  create(input: ClaimInput) {
    return this.write(base, 'POST', input);
  }
  update(id: string, input: ClaimInput, etag: string) {
    return this.write(`${base}/${encodeURIComponent(id)}`, 'PUT', input, etag);
  }
  submit(id: string, etag: string) {
    return this.write(
      `${base}/${encodeURIComponent(id)}/submit`,
      'POST',
      undefined,
      etag,
    );
  }
  transition(id: string, status: ClaimStatus, etag: string) {
    return this.write(
      `${base}/${encodeURIComponent(id)}/status`,
      'PATCH',
      { status },
      etag,
    );
  }
}
