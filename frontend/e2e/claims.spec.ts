import { createHmac } from 'node:crypto';
import { test, expect, type Page } from '@playwright/test';
function token(subject: string, scope: string) {
  const body = `${Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url')}.${Buffer.from(JSON.stringify({ sub: subject, scope, iss: 'claimiq-demo', aud: 'claimiq-api', exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}`;
  return `${body}.${createHmac('sha256', Buffer.alloc(32, 7)).update(body).digest('base64url')}`;
}
async function connect(page: Page, subject: string, scope: string) {
  await page.getByLabel('Access token').fill(token(subject, scope));
  await page.getByRole('button', { name: 'Connect workspace' }).click();
  await expect(
    page.getByRole('heading', { name: 'Claims workspace.' }),
  ).toBeVisible();
}
test('writer creates and edits, submits; reviewer approves through the real API', async ({
  page,
}) => {
  await page.goto('/');
  await connect(page, 'demo-writer', 'claims:read claims:write');
  await page.getByRole('button', { name: 'New claim' }).click();
  await page.getByLabel('Patient reference', { exact: true }).fill('PAT-E2E');
  await page.getByLabel('Provider reference', { exact: true }).fill('PROV-E2E');
  await page.getByLabel('Service date', { exact: true }).fill('2026-01-01');
  await page
    .getByRole('dialog')
    .getByRole('tab', { name: 'Insurance', exact: true })
    .click();
  await page.getByLabel('Payer name', { exact: true }).fill('Demo payer');
  await page.getByLabel('Plan reference', { exact: true }).fill('PLAN-E2E');
  await page.getByLabel('Member reference', { exact: true }).fill('MEM-E2E');
  await page
    .getByRole('dialog')
    .getByRole('tab', { name: 'Clinical context', exact: true })
    .click();
  await page.getByLabel('Diagnosis codes').fill('Z00.00');
  await page
    .getByLabel('Clinical summary (optional)')
    .fill('Synthetic supporting context');
  await page
    .getByRole('dialog')
    .getByRole('tab', { name: 'Billed services', exact: true })
    .click();
  await page.getByLabel('Unit price 1', { exact: true }).fill('125.50');
  await page.getByLabel('Procedure code 1', { exact: true }).fill('DEMO-001');
  await page
    .getByLabel('Service description 1', { exact: true })
    .fill('Synthetic consultation');

  await page.getByRole('button', { name: 'Create draft', exact: true }).click();
  await expect(page.getByRole('heading', { name: /CLM-/ })).toBeVisible();
  await page.getByRole('button', { name: 'Assess denial risk' }).click();
  await expect(
    page.getByRole('region', { name: 'Pre-submission risk assessment' }),
  ).toContainText('synthetic-logistic-v1');
  await expect(
    page.getByText('Coverage status is unknown', { exact: true }),
  ).toBeVisible();
  await page.getByRole('tab', { name: 'Insurance', exact: true }).click();
  await expect(
    page.getByRole('region', { name: 'Assessment information' }),
  ).toContainText('PLAN-E2E');
  await page.getByRole('button', { name: 'Edit draft' }).click();
  await page
    .getByRole('dialog')
    .getByRole('tab', { name: 'Billed services', exact: true })
    .click();
  await page.getByLabel('Unit price 1', { exact: true }).fill('150.25');
  await page.getByRole('button', { name: 'Save changes' }).click();
  await expect(page.locator('.detail-panel .notice.success')).toHaveText(
    'Draft updated.',
  );
  await expect(page.getByText(/Model synthetic-logistic-v1/)).toHaveCount(0);
  await page.getByRole('button', { name: 'Submit claim', exact: true }).click();
  await page.getByRole('button', { name: 'Confirm', exact: true }).click();
  await expect(page.locator('.detail-topline')).toContainText('Submitted');
  await expect(page.getByRole('button', { name: 'Edit draft' })).toHaveCount(0);
  await page.getByRole('button', { name: 'Disconnect' }).click();
  await connect(page, 'demo-reviewer', 'claims:read claims:review');
  await expect(page.getByRole('button', { name: 'New claim' })).toHaveCount(0);
  await page.locator('.claim-link').first().click();
  await page.getByRole('button', { name: 'Start review', exact: true }).click();
  await page.getByRole('button', { name: 'Confirm', exact: true }).click();
  await expect(page.locator('.detail-topline')).toContainText('Under review');
  await page
    .getByRole('button', { name: 'Approve claim', exact: true })
    .click();
  await page.getByRole('button', { name: 'Confirm', exact: true }).click();
  await expect(page.locator('.detail-topline')).toContainText('Approved');
  await page.screenshot({ path: 'test-results/claims-workspace-desktop.png' });
  await page.setViewportSize({ width: 390, height: 844 });
  await expect
    .poll(() =>
      page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    )
    .toBe(true);
  await page.screenshot({ path: 'test-results/claims-workspace-mobile.png' });
});
test('invalid credentials do not open the workspace', async ({ page }) => {
  await page.goto('/');
  await page
    .getByLabel('Access token')
    .fill(token('intruder', 'claims:read') + 'tampered');
  await page.getByRole('button', { name: 'Connect workspace' }).click();
  await expect(page.getByRole('alert')).toContainText('invalid');
  await expect(
    page.getByRole('heading', { name: 'Claims workspace.' }),
  ).toHaveCount(0);
});

const draft = {
  patientReference: 'PAT-CONCURRENT',
  providerReference: 'PROV-1',
  amount: 100,
  currency: 'USD',
  serviceDate: '2026-01-01',
  context: {
    insurance: {
      payerName: 'Demo payer',
      planReference: 'PLAN-DEMO',
      memberReference: 'MEM-DEMO',
      coverageStatus: 'UNKNOWN',
      coverageVerifiedOn: null,
      authorizationStatus: 'NOT_REQUIRED',
      authorizationReference: null,
    },
    diagnosisCodes: ['Z00.00'],
    serviceLines: [
      {
        codeSystem: 'LOCAL',
        procedureCode: 'DEMO-001',
        description: 'Synthetic service',
        units: 1,
        unitPrice: 100,
      },
    ],
    clinicalSummary: 'Synthetic context',
  },
};
test('stale edits require an explicit reload and never overwrite another session', async ({
  page,
  request,
}) => {
  const access = token('concurrent-writer', 'claims:read claims:write');
  const headers = { Authorization: `Bearer ${access}` };
  const response = await request.post('/api/v1/claims', {
    headers,
    data: draft,
  });
  expect(response.status()).toBe(201);
  const claim = (await response.json()) as { id: string };
  await page.goto('/');
  await connect(page, 'concurrent-writer', 'claims:read claims:write');
  await page.locator('.claim-link').first().click();
  await page.getByRole('button', { name: 'Edit draft' }).click();
  await page
    .getByRole('dialog')
    .getByRole('tab', { name: 'Billed services', exact: true })
    .click();
  await page.getByLabel('Unit price 1', { exact: true }).fill('200');
  const concurrent = await request.put(`/api/v1/claims/${claim.id}`, {
    headers: { ...headers, 'If-Match': response.headers()['etag'] },
    data: {
      ...draft,
      amount: 300,
      context: {
        ...draft.context,
        serviceLines: [{ ...draft.context.serviceLines[0], unitPrice: 300 }],
      },
    },
  });
  expect(concurrent.status()).toBe(200);
  await page.getByRole('button', { name: 'Save changes' }).click();
  await expect(page.getByRole('alert')).toContainText(
    'changed in another session',
  );
  await expect(
    page.getByRole('button', { name: 'Save changes' }),
  ).toBeDisabled();
  await page.getByRole('button', { name: 'Reload latest claim' }).click();
  await page
    .getByRole('alertdialog')
    .getByRole('button', { name: 'Discard and leave' })
    .click();
  await expect(page.locator('.claim-value')).toContainText('300.00');
});
test('pagination and owner visibility use server results', async ({
  page,
  request,
}) => {
  const headers = {
    Authorization: `Bearer ${token('paged-writer', 'claims:read claims:write')}`,
  };
  for (let i = 0; i < 11; i++)
    expect(
      (
        await request.post('/api/v1/claims', {
          headers,
          data: { ...draft, patientReference: `PAT-${i}` },
        })
      ).status(),
    ).toBe(201);
  await page.goto('/');
  await connect(page, 'paged-writer', 'claims:read claims:write');
  await expect(page.locator('tbody tr')).toHaveCount(10);
  await expect(page.locator('.table-footer')).toContainText(
    /1.10 of 11 claims/,
  );
  await page.getByRole('button', { name: 'Next', exact: true }).click();
  await expect(page.locator('tbody tr')).toHaveCount(1);
  await page.locator('.claim-link').first().click();
  await expect(page).toHaveURL(/\/claims\/[^/?]+\?returnPage=1$/);
  await page.getByRole('link', { name: 'Back to claims', exact: true }).click();
  await expect(page).toHaveURL(/page=1$/);
  await expect(page.locator('tbody tr')).toHaveCount(1);
  await page.getByRole('button', { name: 'Disconnect' }).click();
  await connect(page, 'other-owner', 'claims:read claims:write');
  await expect(
    page.getByRole('heading', { name: 'No claims here yet' }),
  ).toBeVisible();
});

test('workspace menu and popup retain edits and confirm before discarding', async ({
  page,
}) => {
  await page.goto('/');
  await connect(page, 'popup-writer', 'claims:read claims:write');
  await expect(
    page.locator('header').getByText('New claim', { exact: true }),
  ).toHaveCount(0);
  await page.getByRole('button', { name: 'New claim' }).click();
  const dialog = page.getByRole('dialog', { name: 'New claim', exact: true });
  await expect(dialog).toBeVisible();
  await expect(
    dialog.getByRole('button', { name: 'Continue', exact: true }),
  ).toHaveCount(0);
  await dialog
    .getByLabel('Patient reference', { exact: true })
    .fill('PAT-KEEP');
  await dialog.getByRole('tab', { name: 'Insurance', exact: true }).click();
  await dialog
    .getByLabel('Payer name', { exact: true })
    .fill('Payer to retain');
  await dialog.getByRole('tab', { name: 'Claim basics', exact: true }).click();
  await expect(
    dialog.getByLabel('Patient reference', { exact: true }),
  ).toHaveValue('PAT-KEEP');
  await dialog.getByRole('button', { name: 'Close claim form' }).click();
  await expect(page.getByRole('alertdialog')).toContainText(
    'Discard unsaved changes',
  );
  await page.getByRole('button', { name: 'Stay on this page' }).click();
  await expect(
    dialog.getByLabel('Patient reference', { exact: true }),
  ).toHaveValue('PAT-KEEP');
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({ path: 'test-results/claim-popup-mobile.png' });
  await dialog.getByRole('button', { name: 'Close claim form' }).click();
  await page.getByRole('button', { name: 'Discard and leave' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await page.getByRole('button', { name: 'Menu', exact: true }).click();
  await page
    .getByRole('navigation', { name: 'Main menu' })
    .getByRole('link', { name: 'Workflow guide', exact: true })
    .click();
  await expect(
    page.getByRole('heading', { name: 'Claim workflow', exact: true }),
  ).toBeVisible();
  await page.getByRole('button', { name: 'Menu', exact: true }).click();
  await page
    .getByRole('navigation', { name: 'Main menu' })
    .getByRole('link', { name: 'Your session', exact: true })
    .click();
  await expect(
    page.getByRole('heading', { name: 'Your session', exact: true }),
  ).toBeVisible();
});
test('browser Back is guarded when the popup has unsaved information', async ({
  page,
}) => {
  await page.goto('/');
  await connect(page, 'history-writer', 'claims:read claims:write');
  await page.getByRole('button', { name: 'New claim' }).click();
  await page
    .getByLabel('Patient reference', { exact: true })
    .fill('PAT-HISTORY');
  await page.goBack();
  await expect(page.getByRole('alertdialog')).toBeVisible();
  await page.getByRole('button', { name: 'Stay on this page' }).click();
  await expect(
    page.getByRole('dialog', { name: 'New claim', exact: true }),
  ).toBeVisible();
  await page.goBack();
  await page.getByRole('button', { name: 'Discard and leave' }).click();
  await expect(
    page.getByRole('heading', { name: 'Claims workspace.' }),
  ).toBeVisible();
});
