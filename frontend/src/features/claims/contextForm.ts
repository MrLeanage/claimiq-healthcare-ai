import type {
  Claim,
  ClaimContext,
  InsuranceDetails,
  ServiceLine,
} from './model';
export interface LineFields {
  id: string;
  codeSystem: ServiceLine['codeSystem'];
  procedureCode: string;
  description: string;
  units: string;
  unitPrice: string;
}
export interface ContextFields {
  payerName: string;
  planReference: string;
  memberReference: string;
  coverageStatus: InsuranceDetails['coverageStatus'];
  coverageVerifiedOn: string;
  authorizationStatus: InsuranceDetails['authorizationStatus'];
  authorizationReference: string;
  diagnoses: string;
  clinicalSummary: string;
  lines: LineFields[];
}
export const newLine = (): LineFields => ({
  id: crypto.randomUUID(),
  codeSystem: 'LOCAL',
  procedureCode: '',
  description: '',
  units: '1',
  unitPrice: '',
});
// Presence controls save availability, including conditional authorization fields.
export function hasRequiredContextValues(fields: ContextFields): boolean {
  const required = [
    fields.payerName,
    fields.planReference,
    fields.memberReference,
    fields.coverageStatus,
    fields.authorizationStatus,
    fields.diagnoses,
  ];
  if (fields.authorizationStatus === 'APPROVED') {
    required.push(fields.authorizationReference);
  }
  return (
    required.every((value) => value.trim().length > 0) &&
    fields.lines.length > 0 &&
    fields.lines.every((line) =>
      [
        line.codeSystem,
        line.procedureCode,
        line.description,
        line.units,
        line.unitPrice,
      ].every((value) => value.trim().length > 0),
    )
  );
}
export function initialContext(claim?: Claim): ContextFields {
  const context = claim?.context;
  return {
    payerName: context?.insurance.payerName ?? '',
    planReference: context?.insurance.planReference ?? '',
    memberReference: context?.insurance.memberReference ?? '',
    coverageStatus: context?.insurance.coverageStatus ?? 'UNKNOWN',
    coverageVerifiedOn: context?.insurance.coverageVerifiedOn ?? '',
    authorizationStatus: context?.insurance.authorizationStatus ?? 'UNKNOWN',
    authorizationReference: context?.insurance.authorizationReference ?? '',
    diagnoses: context?.diagnosisCodes.join(', ') ?? '',
    clinicalSummary: context?.clinicalSummary ?? '',
    lines: context?.serviceLines.map((line) => ({
      ...line,
      id: crypto.randomUUID(),
      units: String(line.units),
      unitPrice: line.unitPrice.toFixed(2),
    })) ?? [{ ...newLine(), unitPrice: claim?.amount.toFixed(2) ?? '' }],
  };
}
// Integer cents avoid binary floating-point rounding during money calculations.
function cents(price: string): number {
  if (!/^\d+(\.\d{1,2})?$/.test(price))
    throw new Error(
      'Unit price must be positive with at most two decimal places.',
    );
  const [whole, fraction = ''] = price.split('.');
  const value = Number(whole) * 100 + Number(fraction.padEnd(2, '0'));
  if (!Number.isSafeInteger(value) || value <= 0)
    throw new Error('Enter a positive, supported unit price.');
  return value;
}
function lineCents(line: LineFields): number {
  const units = Number(line.units);
  if (!/^\d+$/.test(line.units) || units < 1 || units > 999)
    throw new Error('Units must be a whole number from 1 to 999.');
  const total = cents(line.unitPrice) * units;
  if (!Number.isSafeInteger(total))
    throw new Error('Service total exceeds the supported amount.');
  return total;
}
export function previewTotal(fields: ContextFields): string {
  try {
    return (
      fields.lines.reduce((sum, line) => sum + lineCents(line), 0) / 100
    ).toFixed(2);
  } catch {
    return '0.00';
  }
}
export function validateInsurance(fields: ContextFields): void {
  if (
    !fields.payerName.trim() ||
    !fields.planReference.trim() ||
    !fields.memberReference.trim()
  )
    throw new Error('Payer, plan, and member references are required.');
  if (
    fields.authorizationStatus === 'APPROVED' &&
    !fields.authorizationReference.trim()
  )
    throw new Error('An approved authorization needs a reference.');
  if (
    fields.coverageVerifiedOn &&
    new Date(fields.coverageVerifiedOn + 'T00:00:00').getTime() >
      new Date().setHours(0, 0, 0, 0)
  )
    throw new Error('Coverage check date cannot be in the future.');
}
export function parseDiagnosisCodes(value: string): string[] {
  const codes = value.split(',').map((code) => code.trim().toUpperCase());
  if (
    codes.length < 1 ||
    codes.length > 20 ||
    codes.some((code) => !/^[A-Z0-9][A-Z0-9.-]{0,19}$/.test(code)) ||
    new Set(codes).size !== codes.length
  )
    throw new Error(
      'Enter 1 to 20 distinct diagnosis codes separated by commas.',
    );
  return codes;
}
// Convert editable strings into the typed API payload and derive its total.
export function buildContext(fields: ContextFields): {
  context: ClaimContext;
  amount: number;
} {
  validateInsurance(fields);
  const diagnoses = parseDiagnosisCodes(fields.diagnoses);
  if (fields.lines.length < 1 || fields.lines.length > 50)
    throw new Error('Enter between 1 and 50 billed services.');
  if (
    fields.authorizationStatus === 'APPROVED' &&
    !fields.authorizationReference.trim()
  )
    throw new Error('An approved authorization needs a reference.');
  const total = fields.lines.reduce((sum, line) => sum + lineCents(line), 0);
  if (!Number.isSafeInteger(total) || total > 99999999999999)
    throw new Error('Claim total exceeds 999,999,999,999.99.');
  const lines = fields.lines.map((line) => {
    const procedureCode = line.procedureCode.trim().toUpperCase();
    if (
      !/^[A-Z0-9][A-Z0-9.-]{0,19}$/.test(procedureCode) ||
      !line.description.trim()
    )
      throw new Error('Each service needs a code and description.');
    return {
      codeSystem: line.codeSystem,
      procedureCode,
      description: line.description.trim(),
      units: Number(line.units),
      unitPrice: cents(line.unitPrice) / 100,
    };
  });
  return {
    amount: total / 100,
    context: {
      insurance: {
        payerName: fields.payerName.trim(),
        planReference: fields.planReference.trim(),
        memberReference: fields.memberReference.trim(),
        coverageStatus: fields.coverageStatus,
        coverageVerifiedOn: fields.coverageVerifiedOn || null,
        authorizationStatus: fields.authorizationStatus,
        authorizationReference: fields.authorizationReference.trim() || null,
      },
      diagnosisCodes: diagnoses,
      serviceLines: lines,
      clinicalSummary: fields.clinicalSummary.trim() || null,
    },
  };
}
