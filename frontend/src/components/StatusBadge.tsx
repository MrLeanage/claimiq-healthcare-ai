import { statusLabels, type ClaimStatus } from '../features/claims/model';
export function StatusBadge({ status }: { status: ClaimStatus }) {
  return (
    <span className={`status status-${status.toLowerCase()}`}>
      <span aria-hidden="true" />
      {statusLabels[status]}
    </span>
  );
}
