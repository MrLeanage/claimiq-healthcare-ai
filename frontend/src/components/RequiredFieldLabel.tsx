import type { ReactNode } from 'react';

// Visual marker only. The input required attribute provides its accessible required state.
export function RequiredFieldLabel({
  children,
  required = true,
}: {
  children: ReactNode;
  required?: boolean;
}) {
  return (
    <span>
      {children}
      {required && (
        <span className="required-marker" aria-hidden="true">
          {' '}
          *
        </span>
      )}
    </span>
  );
}
