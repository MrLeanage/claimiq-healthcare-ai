import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, it, vi } from 'vitest';
import { ClaimForm } from './ClaimForm';
it('validates decimal precision before calling the service contract', async () => {
  const user = userEvent.setup();
  const save = vi.fn().mockResolvedValue(undefined);
  render(<ClaimForm busy={false} onSave={save} onCancel={() => {}} />);
  await user.type(screen.getByLabelText('Patient reference'), 'PAT-1');
  await user.type(screen.getByLabelText('Provider reference'), 'PROV-1');
  await user.type(screen.getByLabelText('Service date'), '2026-01-01');
  await user.click(screen.getByRole('tab', { name: 'Insurance' }));
  await user.type(screen.getByLabelText('Payer name'), 'Demo payer');
  await user.type(screen.getByLabelText('Plan reference'), 'PLAN-DEMO');
  await user.type(screen.getByLabelText('Member reference'), 'MEM-DEMO');
  await user.click(screen.getByRole('tab', { name: 'Clinical context' }));
  await user.type(screen.getByLabelText('Diagnosis codes'), 'Z00.00');
  await user.click(screen.getByRole('tab', { name: 'Billed services' }));
  await user.type(screen.getByLabelText('Unit price 1'), '12.345');
  await user.type(screen.getByLabelText('Procedure code 1'), 'DEMO-001');
  await user.type(
    screen.getByLabelText('Service description 1'),
    'Synthetic consultation',
  );
  await user.click(screen.getByRole('button', { name: 'Create draft' }));
  expect(save).not.toHaveBeenCalled();
  expect(screen.getByRole('alert')).toHaveTextContent('two decimal');
  await user.clear(screen.getByLabelText('Unit price 1'));
  await user.type(screen.getByLabelText('Unit price 1'), '12.34');
  await user.click(screen.getByRole('button', { name: 'Create draft' }));
  expect(save).toHaveBeenCalledWith(
    expect.objectContaining({
      patientReference: 'PAT-1',
      providerReference: 'PROV-1',
      amount: 12.34,
      currency: 'USD',
      serviceDate: '2026-01-01',
      context: expect.objectContaining({
        diagnosisCodes: ['Z00.00'],
        serviceLines: [expect.objectContaining({ units: 1, unitPrice: 12.34 })],
      }),
    }),
  );
});

it('allows direct section access and directs invalid saving to the missing section', async () => {
  const user = userEvent.setup();
  const save = vi.fn().mockResolvedValue(undefined);
  render(<ClaimForm busy={false} onSave={save} onCancel={() => {}} />);
  await user.type(screen.getByLabelText('Patient reference'), 'PAT-RETAINED');
  await user.type(screen.getByLabelText('Provider reference'), 'PROV-1');
  await user.type(screen.getByLabelText('Service date'), '2026-01-01');
  await user.click(screen.getByRole('tab', { name: 'Billed services' }));
  expect(screen.getByLabelText('Unit price 1')).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Create draft' }));
  expect(screen.getByRole('tab', { name: 'Insurance' })).toHaveAttribute(
    'aria-selected',
    'true',
  );
  expect(save).not.toHaveBeenCalled();
  await user.click(screen.getByRole('tab', { name: 'Claim basics' }));
  expect(screen.getByLabelText('Patient reference')).toHaveValue(
    'PAT-RETAINED',
  );
});
