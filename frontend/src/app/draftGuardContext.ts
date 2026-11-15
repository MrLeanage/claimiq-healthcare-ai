import { createContext, useContext } from 'react';
export interface DraftGuard {
  markDirty(value: boolean): void;
  markSaving(value: boolean): void;
  confirmLeave(action: () => void): void;
}
export const DraftGuardContext = createContext<DraftGuard | null>(null);
export function useDraftGuard(): DraftGuard {
  const guard = useContext(DraftGuardContext);
  if (!guard) throw new Error('Draft guard provider is required');
  return guard;
}
