export interface InsuranceDetails {
  readonly payerName: string;
  readonly planReference: string;
  readonly memberReference: string;
  readonly coverageStatus: 'UNKNOWN' | 'ACTIVE' | 'INACTIVE';
  readonly coverageVerifiedOn: string | null;
  readonly authorizationStatus:
    'UNKNOWN' | 'NOT_REQUIRED' | 'PENDING' | 'APPROVED' | 'DENIED';
  readonly authorizationReference: string | null;
}
export interface ServiceLine {
  readonly codeSystem: 'LOCAL' | 'CPT' | 'HCPCS';
  readonly procedureCode: string;
  readonly description: string;
  readonly units: number;
  readonly unitPrice: number;
}
export interface ClaimContext {
  readonly insurance: InsuranceDetails;
  readonly diagnosisCodes: readonly string[];
  readonly serviceLines: readonly ServiceLine[];
  readonly clinicalSummary: string | null;
}
export type ClaimStatus =
  'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';
export interface Claim {
  readonly id: string;
  readonly claimNumber: string;
  readonly patientReference: string;
  readonly providerReference: string;
  readonly amount: number;
  readonly currency: string;
  readonly serviceDate: string;
  readonly status: ClaimStatus;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly submittedAt: string | null;
  readonly version: number;
  readonly context: ClaimContext | null;
}
export interface ClaimInput {
  patientReference: string;
  providerReference: string;
  amount: number;
  currency: string;
  serviceDate: string;
  context: ClaimContext;
}
export interface ClaimPage {
  content: Claim[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
export interface VersionedClaim {
  claim: Claim;
  etag: string;
}
export interface ClaimsGateway {
  assess?(
    id: string,
    etag: string,
    signal?: AbortSignal,
  ): Promise<import('./assessment').Assessment>;
  list(page: number, signal?: AbortSignal): Promise<ClaimPage>;
  get(id: string, signal?: AbortSignal): Promise<VersionedClaim>;
  create(input: ClaimInput): Promise<VersionedClaim>;
  update(id: string, input: ClaimInput, etag: string): Promise<VersionedClaim>;
  submit(id: string, etag: string): Promise<VersionedClaim>;
  transition(
    id: string,
    status: ClaimStatus,
    etag: string,
  ): Promise<VersionedClaim>;
}
export const statusLabels: Record<ClaimStatus, string> = {
  DRAFT: 'Draft',
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
};
export function reviewActions(status: ClaimStatus): ClaimStatus[] {
  return status === 'SUBMITTED'
    ? ['UNDER_REVIEW']
    : status === 'UNDER_REVIEW'
      ? ['APPROVED', 'REJECTED']
      : [];
}
export function formatAmount(amount: number, currency: string): string {
  // Never add values across currencies. Formatting preserves each claim's currency.
  try {
    return new Intl.NumberFormat('en', { style: 'currency', currency }).format(
      amount,
    );
  } catch {
    return `${amount.toFixed(2)} ${currency}`;
  }
}
