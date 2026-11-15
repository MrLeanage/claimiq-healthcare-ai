import { useRef, useState, type FormEvent } from 'react';
import type { Claim, ClaimInput } from './model';
import {
  initialContext,
  buildContext,
  previewTotal,
  validateInsurance,
  parseDiagnosisCodes,
} from './contextForm';
import { ClaimContextFields } from './ClaimContextFields';
import { ClaimContextView } from './ClaimContextView';
import { Tabs } from '../../components/Tabs';
interface Props {
  initial?: Claim;
  busy: boolean;
  locked?: boolean;
  onSave(input: ClaimInput): Promise<void>;
  onCancel(): void;
  onDirtyChange?(dirty: boolean): void;
}
type Section = 'basics' | 'insurance' | 'clinical' | 'services' | 'review';
const sections = [
  { value: 'basics', label: 'Claim basics' },
  { value: 'insurance', label: 'Insurance' },
  { value: 'clinical', label: 'Clinical context' },
  { value: 'services', label: 'Billed services' },
  { value: 'review', label: 'Review' },
] as const;
export function ClaimForm({
  initial,
  busy,
  locked = false,
  onSave,
  onCancel,
  onDirtyChange,
}: Props) {
  const [fields, setFields] = useState({
    patientReference: initial?.patientReference ?? '',
    providerReference: initial?.providerReference ?? '',
    currency: initial?.currency ?? 'USD',
    serviceDate: initial?.serviceDate ?? '',
  });
  const [context, setContext] = useState(() => initialContext(initial));
  const [section, setSection] = useState<Section>('basics');
  const [validation, setValidation] = useState('');
  const heading = useRef<HTMLHeadingElement>(null);
  const disabled = busy || locked;
  const today = new Date();
  const maxDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
  function change(field: keyof typeof fields, value: string) {
    setFields((old) => ({ ...old, [field]: value }));
    onDirtyChange?.(true);
  }
  function go(next: Section) {
    setSection(next);
    setValidation('');
  }
  function basicValidation() {
    if (
      !fields.patientReference.trim() ||
      !fields.providerReference.trim() ||
      fields.patientReference.length > 100 ||
      fields.providerReference.length > 100 ||
      !/^[A-Z]{3}$/.test(fields.currency) ||
      !fields.serviceDate ||
      fields.serviceDate > maxDate
    )
      throw new Error(
        'Complete the patient/provider references, currency, and a valid service date.',
      );
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (disabled) return;
    let invalidSection: Section = 'basics';
    try {
      basicValidation();
      invalidSection = 'insurance';
      validateInsurance(context);
      invalidSection = 'clinical';
      parseDiagnosisCodes(context.diagnoses);
      if (context.clinicalSummary.length > 2000)
        throw new Error('Clinical summary exceeds 2,000 characters.');
      invalidSection = 'services';
      const assessment = buildContext(context);
      setValidation('');
      await onSave({
        ...fields,
        patientReference: fields.patientReference.trim(),
        providerReference: fields.providerReference.trim(),
        ...assessment,
      });
    } catch (error) {
      setSection(invalidSection);
      setValidation(
        error instanceof Error ? error.message : 'Check the claim information.',
      );
      requestAnimationFrame(() => heading.current?.focus());
    }
  }
  let review: ReturnType<typeof buildContext> | null = null;
  if (section === 'review') {
    try {
      review = buildContext(context);
    } catch {
      /* An incomplete draft can still be previewed. */
    }
  }
  return (
    <form
      className="claim-form sectioned-form"
      noValidate
      onSubmit={(event) => void submit(event)}
    >
      <Tabs<Section>
        id="claim-entry"
        label="Claim form sections"
        disabled={busy}
        items={sections}
        value={section}
        onChange={go}
      />
      <div
        id="claim-entry-panel"
        role="tabpanel"
        aria-labelledby={`claim-entry-${section}-tab`}
        tabIndex={0}
        className="entry-panel"
      >
        <div className="step-heading">
          <h2 ref={heading} tabIndex={-1}>
            {sections.find((item) => item.value === section)?.label}
          </h2>
          <p className="muted small">
            Move between sections freely. Information is saved only when you
            choose {initial ? 'Save changes' : 'Create draft'}.
          </p>
        </div>
        {validation && (
          <div role="alert" className="notice error">
            {validation}
          </div>
        )}
        {section === 'basics' && (
          <fieldset disabled={disabled} className="basics-fields">
            <legend className="sr-only">Claim basics</legend>
            <div className="form-grid">
              <label>
                Patient reference
                <input
                  required
                  maxLength={100}
                  value={fields.patientReference}
                  onChange={(e) => change('patientReference', e.target.value)}
                  placeholder="PAT-001"
                />
              </label>
              <label>
                Provider reference
                <input
                  required
                  maxLength={100}
                  value={fields.providerReference}
                  onChange={(e) => change('providerReference', e.target.value)}
                  placeholder="PROV-001"
                />
              </label>
              <label>
                Currency
                <input
                  aria-label="Currency"
                  required
                  pattern="[A-Z]{3}"
                  maxLength={3}
                  value={fields.currency}
                  onChange={(e) =>
                    change('currency', e.target.value.toUpperCase())
                  }
                />
              </label>
              <label>
                Service date
                <input
                  type="date"
                  required
                  max={maxDate}
                  value={fields.serviceDate}
                  onChange={(e) => change('serviceDate', e.target.value)}
                />
              </label>
            </div>
            <p className="small muted">
              Use synthetic reference IDs. Do not enter patient names or contact
              information.
            </p>
          </fieldset>
        )}
        {(section === 'insurance' ||
          section === 'clinical' ||
          section === 'services') && (
          <ClaimContextFields
            section={section}
            value={context}
            disabled={disabled}
            maxDate={maxDate}
            onChange={(value) => {
              setContext(value);
              onDirtyChange?.(true);
            }}
          />
        )}
        {section === 'services' && (
          <div className="wizard-total">
            <span>Calculated claim total</span>
            <output aria-label="Claim amount">
              {previewTotal(context)} {fields.currency}
            </output>
          </div>
        )}
        {section === 'review' && (
          <div className="review-step">
            <div className="review-banner">
              Saving keeps this claim as a draft. Submit it separately from
              Claim details when ready.
            </div>
            <dl className="details-grid">
              <div>
                <dt>Patient reference</dt>
                <dd>{fields.patientReference || 'Not provided'}</dd>
              </div>
              <div>
                <dt>Provider reference</dt>
                <dd>{fields.providerReference || 'Not provided'}</dd>
              </div>
              <div>
                <dt>Service date</dt>
                <dd>{fields.serviceDate || 'Not provided'}</dd>
              </div>
              <div>
                <dt>Claim total</dt>
                <dd>
                  {previewTotal(context)} {fields.currency}
                </dd>
              </div>
            </dl>
            {review ? (
              <ClaimContextView
                context={review.context}
                currency={fields.currency}
              />
            ) : (
              <p className="notice context-missing">
                Some details are incomplete. Complete the relevant sections
                before saving.
              </p>
            )}
          </div>
        )}
      </div>
      <div className="form-actions">
        <span className="small muted">Unsaved changes stay in this popup.</span>
        <div className="button-row">
          <button
            type="button"
            className="button secondary"
            disabled={busy}
            onClick={onCancel}
          >
            Cancel
          </button>
          <button className="button primary" disabled={disabled}>
            {busy ? 'Saving...' : initial ? 'Save changes' : 'Create draft'}
          </button>
        </div>
      </div>
    </form>
  );
}
