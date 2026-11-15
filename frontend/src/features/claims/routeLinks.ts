export function pageNumber(value: string | null): number {
  const page = Number(value ?? 0);
  return Number.isSafeInteger(page) && page >= 0 && page <= 1000000 ? page : 0;
}
export function listPath(page: number): string {
  return `/claims?page=${page}`;
}
export function claimPath(id: string, page: number): string {
  return `/claims/${encodeURIComponent(id)}?returnPage=${page}`;
}
