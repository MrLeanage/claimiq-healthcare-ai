import { useRef } from 'react';
export function Tabs<T extends string>({
  id,
  label,
  items,
  value,
  onChange,
  disabled = false,
}: {
  id: string;
  label: string;
  items: readonly { value: T; label: string }[];
  value: T;
  onChange(value: T): void;
  disabled?: boolean;
}) {
  const buttons = useRef<(HTMLButtonElement | null)[]>([]);
  return (
    <div className="section-tabs" role="tablist" aria-label={label}>
      {items.map((item, index) => (
        <button
          type="button"
          disabled={disabled}
          role="tab"
          key={item.value}
          id={`${id}-${item.value}-tab`}
          aria-controls={`${id}-panel`}
          aria-selected={value === item.value}
          tabIndex={value === item.value ? 0 : -1}
          ref={(element) => {
            buttons.current[index] = element;
          }}
          onClick={() => onChange(item.value)}
          onKeyDown={(event) => {
            let next: number;
            if (event.key === 'ArrowRight') next = (index + 1) % items.length;
            else if (event.key === 'ArrowLeft')
              next = (index + items.length - 1) % items.length;
            else if (event.key === 'Home') next = 0;
            else if (event.key === 'End') next = items.length - 1;
            else return;
            event.preventDefault();
            onChange(items[next].value);
            buttons.current[next]?.focus();
          }}
        >
          {item.label}
        </button>
      ))}
    </div>
  );
}
