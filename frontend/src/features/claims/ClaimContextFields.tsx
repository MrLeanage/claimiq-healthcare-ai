import { RequiredFieldLabel } from '../../components/RequiredFieldLabel';
import { newLine, type ContextFields, type LineFields } from './contextForm';
interface Props {
  value: ContextFields;
  disabled: boolean;
  onChange(value: ContextFields): void;
  maxDate: string;
  section?: 'all' | 'insurance' | 'clinical' | 'services';
}
// Props provide current values; onChange sends new copies back to the parent form.
export function ClaimContextFields({
  value,
  disabled,
  onChange,
  maxDate,
  section = 'all',
}: Props) {
  function change<K extends keyof ContextFields>(
    field: K,
    next: ContextFields[K],
  ) {
    onChange({ ...value, [field]: next });
  }
  function lineChange(index: number, patch: Partial<LineFields>) {
    change(
      'lines',
      value.lines.map((line, i) =>
        i === index ? { ...line, ...patch } : line,
      ),
    );
  }
  return (
    <>
      {(section === 'all' || section === 'insurance') && (
        <fieldset className="claim-section" disabled={disabled}>
          <legend>Insurance and authorization</legend>
          <p className="small muted">
            Reported information only. ClaimIQ does not verify eligibility with
            a payer.
          </p>
          <div className="form-grid">
            <label>
              <RequiredFieldLabel>Payer name</RequiredFieldLabel>
              <input
                aria-label="Payer name"
                required
                maxLength={150}
                value={value.payerName}
                onChange={(e) => change('payerName', e.target.value)}
                placeholder="Demo Health Plan"
              />
            </label>
            <label>
              <RequiredFieldLabel>Plan reference</RequiredFieldLabel>
              <input
                aria-label="Plan reference"
                required
                maxLength={100}
                value={value.planReference}
                onChange={(e) => change('planReference', e.target.value)}
                placeholder="PLAN-DEMO"
              />
            </label>
            <label>
              <RequiredFieldLabel>Member reference</RequiredFieldLabel>
              <input
                aria-label="Member reference"
                required
                maxLength={100}
                value={value.memberReference}
                onChange={(e) => change('memberReference', e.target.value)}
                placeholder="MEM-DEMO-001"
              />
            </label>
            <label>
              <RequiredFieldLabel>Reported coverage status</RequiredFieldLabel>
              <select
                required
                aria-label="Reported coverage status"
                value={value.coverageStatus}
                onChange={(e) =>
                  change(
                    'coverageStatus',
                    e.target.value as ContextFields['coverageStatus'],
                  )
                }
              >
                <option value="UNKNOWN">Unknown / not checked</option>
                <option value="ACTIVE">Reported active</option>
                <option value="INACTIVE">Reported inactive</option>
              </select>
            </label>
            <label>
              Coverage check date (optional)
              <input
                type="date"
                max={maxDate}
                value={value.coverageVerifiedOn}
                onChange={(e) => change('coverageVerifiedOn', e.target.value)}
              />
            </label>
            <label>
              <RequiredFieldLabel>Authorization status</RequiredFieldLabel>
              <select
                required
                aria-label="Authorization status"
                value={value.authorizationStatus}
                onChange={(e) =>
                  change(
                    'authorizationStatus',
                    e.target.value as ContextFields['authorizationStatus'],
                  )
                }
              >
                {[
                  'UNKNOWN',
                  'NOT_REQUIRED',
                  'PENDING',
                  'APPROVED',
                  'DENIED',
                ].map((status) => (
                  <option key={status} value={status}>
                    {status.replaceAll('_', ' ').toLowerCase()}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <RequiredFieldLabel
                required={value.authorizationStatus === 'APPROVED'}
              >
                Authorization reference
              </RequiredFieldLabel>
              <input
                aria-label="Authorization reference"
                required={value.authorizationStatus === 'APPROVED'}
                maxLength={100}
                value={value.authorizationReference}
                onChange={(e) =>
                  change('authorizationReference', e.target.value)
                }
                placeholder="Required when approved"
              />
            </label>
          </div>
        </fieldset>
      )}
      {(section === 'all' || section === 'clinical') && (
        <fieldset className="claim-section" disabled={disabled}>
          <legend>Diagnosis information</legend>
          <label>
            <RequiredFieldLabel>Diagnosis codes</RequiredFieldLabel>
            <input
              required
              maxLength={420}
              aria-label="Diagnosis codes"
              aria-describedby="diagnosis-help"
              value={value.diagnoses}
              onChange={(e) =>
                change('diagnoses', e.target.value.toUpperCase())
              }
              placeholder="Z00.00"
            />
            <small id="diagnosis-help">
              ICD-10-CM identifiers, separated by commas (maximum 20). Code
              validity is not verified.
            </small>
          </label>
        </fieldset>
      )}
      {(section === 'all' || section === 'services') && (
        <fieldset className="claim-section" disabled={disabled}>
          <legend>Billed services</legend>
          <p className="small muted">
            The claim amount is calculated from units x unit price. LOCAL codes
            are available for synthetic examples.
          </p>
          {value.lines.map((line, index) => (
            <fieldset className="service-line" key={line.id}>
              <legend>Service {index + 1}</legend>
              <div className="form-grid">
                <label>
                  <RequiredFieldLabel>
                    Code system {index + 1}
                  </RequiredFieldLabel>
                  <select
                    required
                    aria-label={`Code system ${index + 1}`}
                    value={line.codeSystem}
                    onChange={(e) =>
                      lineChange(index, {
                        codeSystem: e.target.value as LineFields['codeSystem'],
                      })
                    }
                  >
                    <option value="LOCAL">Local / demo</option>
                    <option value="CPT">CPT</option>
                    <option value="HCPCS">HCPCS</option>
                  </select>
                </label>
                <label>
                  <RequiredFieldLabel>
                    Procedure code {index + 1}
                  </RequiredFieldLabel>
                  <input
                    aria-label={`Procedure code ${index + 1}`}
                    required
                    maxLength={20}
                    value={line.procedureCode}
                    onChange={(e) =>
                      lineChange(index, {
                        procedureCode: e.target.value.toUpperCase(),
                      })
                    }
                    placeholder="DEMO-001"
                  />
                </label>
                <label className="span-two">
                  <RequiredFieldLabel>
                    Service description {index + 1}
                  </RequiredFieldLabel>
                  <input
                    aria-label={`Service description ${index + 1}`}
                    required
                    maxLength={200}
                    value={line.description}
                    onChange={(e) =>
                      lineChange(index, { description: e.target.value })
                    }
                    placeholder="Synthetic consultation"
                  />
                </label>
                <label>
                  <RequiredFieldLabel>Units {index + 1}</RequiredFieldLabel>
                  <input
                    aria-label={`Units ${index + 1}`}
                    type="number"
                    required
                    min={1}
                    max={999}
                    step={1}
                    value={line.units}
                    onChange={(e) =>
                      lineChange(index, { units: e.target.value })
                    }
                  />
                </label>
                <label>
                  <RequiredFieldLabel>
                    Unit price {index + 1}
                  </RequiredFieldLabel>
                  <input
                    aria-label={`Unit price ${index + 1}`}
                    required
                    inputMode="decimal"
                    value={line.unitPrice}
                    onChange={(e) =>
                      lineChange(index, { unitPrice: e.target.value })
                    }
                    placeholder="125.50"
                  />
                </label>
              </div>
              <button
                type="button"
                className="button text"
                disabled={value.lines.length === 1}
                onClick={() =>
                  change(
                    'lines',
                    value.lines.filter((_, i) => i !== index),
                  )
                }
              >
                Remove service {index + 1}
              </button>
            </fieldset>
          ))}
          <button
            type="button"
            className="button secondary compact"
            disabled={value.lines.length >= 50}
            onClick={() => change('lines', [...value.lines, newLine()])}
          >
            Add service
          </button>
        </fieldset>
      )}
      {(section === 'all' || section === 'clinical') && (
        <fieldset className="claim-section" disabled={disabled}>
          <legend>Supporting context</legend>
          <label>
            Clinical summary (optional)
            <textarea
              rows={4}
              maxLength={2000}
              aria-label="Clinical summary (optional)"
              value={value.clinicalSummary}
              onChange={(e) => change('clinicalSummary', e.target.value)}
              placeholder="Synthetic context explaining the billed services"
            />
            <small>
              Maximum 2,000 characters. Use synthetic information only; omit
              names and contact details.
            </small>
          </label>
        </fieldset>
      )}
    </>
  );
}
