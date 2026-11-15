import { expect, it } from 'vitest';
import { initialContext, buildContext, previewTotal } from './contextForm';
function valid() {
  const form = initialContext();
  return {
    ...form,
    payerName: 'Demo payer',
    planReference: 'PLAN',
    memberReference: 'MEM',
    diagnoses: 'Z00.00',
    lines: [
      {
        ...form.lines[0],
        procedureCode: 'DEMO-001',
        description: 'Synthetic service',
        units: '3',
        unitPrice: '0.10',
      },
    ],
  };
}
it('uses integer cents for the calculated total and normalizes code identifiers', () => {
  const input = valid();
  input.diagnoses = ' z00.00, r69 ';
  expect(buildContext(input).amount).toBe(0.3);
  expect(buildContext(input).context.diagnosisCodes).toEqual(['Z00.00', 'R69']);
  expect(previewTotal(input)).toBe('0.30');
});
it('rejects duplicate codes, invalid units, fractional cents, and missing authorization references', () => {
  const input = valid();
  expect(() => buildContext({ ...input, diagnoses: 'Z00.00, Z00.00' })).toThrow(
    'distinct',
  );
  expect(() =>
    buildContext({ ...input, lines: [{ ...input.lines[0], units: '1.5' }] }),
  ).toThrow('whole number');
  expect(() =>
    buildContext({
      ...input,
      lines: [{ ...input.lines[0], unitPrice: '0.001' }],
    }),
  ).toThrow('decimal');
  expect(() =>
    buildContext({ ...input, authorizationStatus: 'APPROVED' }),
  ).toThrow('reference');
});
