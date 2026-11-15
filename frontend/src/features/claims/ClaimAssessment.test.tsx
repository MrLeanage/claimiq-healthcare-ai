import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { ClaimAssessment } from './ClaimAssessment';
import type { ClaimsGateway, VersionedClaim } from './model';
import type { Assessment } from './assessment';

const current = {
  claim: { id: 'demo', version: 2, context: {} },
  etag: '"2"',
} as VersionedClaim;
const result: Assessment = {
  claimId: 'demo',
  claimVersion: 2,
  assessment: {
    modelVersion: 'synthetic-logistic-v1',
    trainingData: 'SYNTHETIC',
    assessedAt: '2026-11-15T00:00:00Z',
    score: 80,
    riskLevel: 'HIGH',
    interceptLogOdds: -3,
    limitations: ['Demo only'],
    findings: [
      {
        code: 'authorization_pending',
        title: 'Authorization is pending',
        field: 'insurance.authorizationStatus',
        evidence: 'PENDING',
        contributionLogOdds: 1.8,
        suggestedCorrection: 'Confirm authorization with the payer.',
      },
    ],
  },
};

describe('claim assessment', () => {
  it('shows evidence and correction for the current version and hides stale results', async () => {
    const assess = vi.fn().mockResolvedValue(result);
    const api = { assess } as unknown as ClaimsGateway;
    const view = render(
      <ClaimAssessment api={api} current={current} disabled={false} />,
    );
    await userEvent.click(
      screen.getByRole('button', { name: 'Assess denial risk' }),
    );
    expect(
      await screen.findByText('Authorization is pending'),
    ).toBeInTheDocument();
    expect(
      screen.getByText('Confirm authorization with the payer.'),
    ).toBeInTheDocument();
    expect(assess).toHaveBeenCalledWith('demo', '"2"', expect.any(AbortSignal));
    view.rerender(
      <ClaimAssessment
        api={api}
        current={{ ...current, claim: { ...current.claim, version: 3 } }}
        disabled={false}
      />,
    );
    expect(
      screen.queryByText('Authorization is pending'),
    ).not.toBeInTheDocument();
    expect(
      screen.getByText('The claim changed. Run a new assessment.'),
    ).toBeInTheDocument();
  });
  it('does not display a low score on failure', async () => {
    const api = {
      assess: vi.fn().mockRejectedValue(new Error('offline')),
    } as unknown as ClaimsGateway;
    render(<ClaimAssessment api={api} current={current} disabled={false} />);
    await userEvent.click(
      screen.getByRole('button', { name: 'Assess denial risk' }),
    );
    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(screen.queryByText(/LOW risk/)).not.toBeInTheDocument();
  });
});
