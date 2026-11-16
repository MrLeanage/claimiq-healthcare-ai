import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, it, vi } from 'vitest';
import { ClaimForm } from './ClaimForm';
import type { Claim } from './model';
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

it('keeps saving disabled when required fields in other sections are empty', async () => {
  const user = userEvent.setup();
  const save = vi.fn().mockResolvedValue(undefined);
  render(<ClaimForm busy={false} onSave={save} onCancel={() => {}} />);
  await user.type(screen.getByLabelText('Patient reference'), 'PAT-RETAINED');
  await user.type(screen.getByLabelText('Provider reference'), 'PROV-1');
  await user.type(screen.getByLabelText('Service date'), '2026-01-01');
  await user.click(screen.getByRole('tab', { name: 'Billed services' }));
  expect(screen.getByLabelText('Unit price 1')).toBeVisible();
  expect(screen.getByRole('button', { name: 'Create draft' })).toBeDisabled();
  expect(
    screen.getByText(
      'Complete required fields in all sections to save the draft.',
    ),
  ).toBeVisible();
  fireEvent.submit(
    screen.getByRole('button', { name: 'Create draft' }).closest('form')!,
  );
  expect(save).not.toHaveBeenCalled();
  await user.click(screen.getByRole('tab', { name: 'Claim basics' }));
  expect(screen.getByLabelText('Patient reference')).toHaveValue(
    'PAT-RETAINED',
  );
});

const existing: Claim = {
  id: 'demo',
  claimNumber: 'CLM-DEMO',
  patientReference: 'PAT-DEMO',
  providerReference: 'PROV-DEMO',
  currency: 'USD',
  serviceDate: '2026-01-01',
  amount: 125.5,
  status: 'DRAFT',
  version: 0,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
  submittedAt: null,
  context: {
    insurance: {
      payerName: 'Demo payer',
      planReference: 'PLAN',
      memberReference: 'MEM',
      coverageStatus: 'UNKNOWN',
      coverageVerifiedOn: null,
      authorizationStatus: 'UNKNOWN',
      authorizationReference: null,
    },
    diagnosisCodes: ['Z00.00'],
    clinicalSummary: null,
    serviceLines: [
      {
        codeSystem: 'LOCAL',
        procedureCode: 'DEMO',
        description: 'Synthetic service',
        units: 1,
        unitPrice: 125.5,
      },
    ],
  },
};

it('disables editing for missing values and updates conditional authorization requirements', async () => {
  const user = userEvent.setup();
  render(
    <ClaimForm
      initial={existing}
      busy={false}
      onSave={vi.fn()}
      onCancel={() => {}}
    />,
  );
  const save = screen.getByRole('button', { name: 'Save changes' });
  expect(save).toBeEnabled();
  const patient = screen.getByLabelText('Patient reference');
  expect(patient).toBeRequired();
  expect(within(patient.closest('label')!).getByText('*')).toHaveAttribute(
    'aria-hidden',
    'true',
  );
  await user.clear(patient);
  await user.type(patient, '   ');
  expect(save).toBeDisabled();
  await user.clear(patient);
  await user.type(patient, 'PAT-DEMO');
  expect(save).toBeEnabled();
  await user.click(screen.getByRole('tab', { name: 'Insurance' }));
  const payer = screen.getByLabelText('Payer name');
  await user.clear(payer);
  await user.click(screen.getByRole('tab', { name: 'Review' }));
  expect(save).toBeDisabled();
  await user.click(screen.getByRole('tab', { name: 'Insurance' }));
  await user.type(screen.getByLabelText('Payer name'), 'Demo payer');
  expect(save).toBeEnabled();
  const reference = screen.getByLabelText('Authorization reference');
  expect(reference).not.toBeRequired();
  expect(
    within(reference.closest('label')!).queryByText('*'),
  ).not.toBeInTheDocument();
  await user.selectOptions(
    screen.getByLabelText('Authorization status'),
    'APPROVED',
  );
  expect(reference).toBeRequired();
  expect(within(reference.closest('label')!).getByText('*')).toBeVisible();
  expect(save).toBeDisabled();
  await user.type(reference, 'AUTH-DEMO');
  expect(save).toBeEnabled();
  await user.clear(reference);
  await user.selectOptions(
    screen.getByLabelText('Authorization status'),
    'NOT_REQUIRED',
  );
  expect(save).toBeEnabled();
});

it('requires every added service and leaves optional fields optional', async () => {
  const user = userEvent.setup();
  render(
    <ClaimForm
      initial={existing}
      busy={false}
      onSave={vi.fn()}
      onCancel={() => {}}
    />,
  );
  const save = screen.getByRole('button', { name: 'Save changes' });
  await user.click(screen.getByRole('tab', { name: 'Clinical context' }));
  const summary = screen.getByLabelText('Clinical summary (optional)');
  expect(summary).not.toBeRequired();
  expect(
    within(summary.closest('label')!).queryByText('*'),
  ).not.toBeInTheDocument();
  await user.clear(screen.getByLabelText('Diagnosis codes'));
  expect(save).toBeDisabled();
  await user.type(screen.getByLabelText('Diagnosis codes'), 'Z00.00');
  expect(save).toBeEnabled();
  await user.click(screen.getByRole('tab', { name: 'Billed services' }));
  await user.click(screen.getByRole('button', { name: 'Add service' }));
  expect(save).toBeDisabled();
  await user.type(screen.getByLabelText('Procedure code 2'), 'DEMO-2');
  await user.type(
    screen.getByLabelText('Service description 2'),
    'Second service',
  );
  await user.type(screen.getByLabelText('Unit price 2'), '10.00');
  expect(save).toBeEnabled();
  await user.clear(screen.getByLabelText('Units 2'));
  expect(save).toBeDisabled();
  await user.click(screen.getByRole('button', { name: 'Remove service 2' }));
  expect(save).toBeEnabled();
});
