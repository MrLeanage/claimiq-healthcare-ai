import { expect, it } from 'vitest';
import { reviewActions } from './model';
it('offers review actions only at the appropriate lifecycle stage', () => {
  expect(reviewActions('DRAFT')).toEqual([]);
  expect(reviewActions('SUBMITTED')).toEqual(['UNDER_REVIEW']);
  expect(reviewActions('UNDER_REVIEW')).toEqual(['APPROVED', 'REJECTED']);
  expect(reviewActions('APPROVED')).toEqual([]);
  expect(reviewActions('REJECTED')).toEqual([]);
});
