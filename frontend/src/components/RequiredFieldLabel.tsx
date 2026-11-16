import type { ReactNode } from 'react';

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
