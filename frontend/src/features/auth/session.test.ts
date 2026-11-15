import { describe, expect, it } from 'vitest';
import { parseSession } from './session';
function token(payload: object) {
  return `header.${btoa(JSON.stringify(payload)).replace(/=/g, '')}.signature`;
}
describe('session hints', () => {
  it('extracts a subject and scopes without treating decoding as signature validation', () => {
    const session = parseSession(
      token({
        sub: 'writer',
        scope: 'claims:read claims:write',
        exp: Date.now() / 1000 + 3600,
      }),
    );
    expect(session.subject).toBe('writer');
    expect(session.scopes).toEqual(['claims:read', 'claims:write']);
  });
  it('supports provider scp arrays', () => {
    expect(
      parseSession(
        token({
          sub: 'reviewer',
          scp: ['claims:review'],
          exp: Date.now() / 1000 + 60,
        }),
      ).scopes,
    ).toEqual(['claims:review']);
  });
  it.each([
    'invalid',
    token({ sub: 'x', exp: 1 }),
    token({ sub: '', exp: Date.now() / 1000 + 60 }),
  ])('rejects invalid or expired token %s', (value) =>
    expect(() => parseSession(value)).toThrow(),
  );
});
