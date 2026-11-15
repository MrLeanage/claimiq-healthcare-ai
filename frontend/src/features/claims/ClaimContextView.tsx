import type { ClaimContext } from './model';
import { formatAmount } from './model';
export function ClaimContextView({
  context,
  currency,
  section = 'all',
}: {
  context: ClaimContext | null;
  currency: string;
  section?: 'all' | 'insurance' | 'clinical' | 'services';
}) {
  if (!context)
    return (
      <div className="notice context-missing">
        This older claim has no recorded insurance, diagnosis, or billed-service
        details. No information has been invented for it. Drafts need these
        details before submission.
      </div>
    );
  const insurance = context.insurance;
  return (
    <section className="context-details" aria-label="Assessment information">
      <h3>Assessment information</h3>
      <p className="small muted">
        User-reported information. Codes and coverage have not been verified.
        The separate risk assessment is advisory and uses synthetic training
        data.
      </p>
      {(section === 'all' || section === 'insurance') && (
        <>
          <h4>Insurance and authorization</h4>
          <dl className="details-grid">
            <div>
              <dt>Payer</dt>
              <dd>{insurance.payerName}</dd>
            </div>
            <div>
              <dt>Plan reference</dt>
              <dd>{insurance.planReference}</dd>
            </div>
            <div>
              <dt>Member reference</dt>
              <dd>{insurance.memberReference}</dd>
            </div>
            <div>
              <dt>Reported coverage</dt>
              <dd>{insurance.coverageStatus.toLowerCase()}</dd>
            </div>
            <div>
              <dt>Coverage check date</dt>
              <dd>{insurance.coverageVerifiedOn ?? 'Not recorded'}</dd>
            </div>
            <div>
              <dt>Authorization</dt>
              <dd>
                {insurance.authorizationStatus
                  .replaceAll('_', ' ')
                  .toLowerCase()}
                {insurance.authorizationReference &&
                  ` (${insurance.authorizationReference})`}
              </dd>
            </div>
          </dl>
        </>
      )}
      {(section === 'all' || section === 'clinical') && (
        <>
          <h4>Diagnosis codes</h4>
          <div className="diagnosis-list">
            {context.diagnosisCodes.map((code) => (
              <span key={code}>{code}</span>
            ))}
          </div>
        </>
      )}
      {(section === 'all' || section === 'services') && (
        <>
          <h4>Billed services</h4>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Service</th>
                  <th>Units</th>
                  <th>Unit price</th>
                  <th>Total</th>
                </tr>
              </thead>
              <tbody>
                {context.serviceLines.map((line, index) => (
                  <tr key={index}>
                    <td>
                      <strong>{line.procedureCode}</strong>
                      <span className="cell-subtitle">
                        {line.codeSystem} - {line.description}
                      </span>
                    </td>
                    <td>{line.units}</td>
                    <td>{formatAmount(line.unitPrice, currency)}</td>
                    <td>
                      {formatAmount(line.unitPrice * line.units, currency)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
      {(section === 'all' || section === 'clinical') && (
        <>
          <h4>Supporting context</h4>
          <p className="clinical-summary">
            {context.clinicalSummary || 'No clinical summary recorded.'}
          </p>
        </>
      )}
    </section>
  );
}
