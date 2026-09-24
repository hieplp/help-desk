interface FieldLike {
  state: { value: string; meta: { errors: unknown[] } }
  handleBlur: () => void
  handleChange: (value: string) => void
}

export function FormField({
  field,
  label,
  type = 'text',
  autoComplete,
  placeholder,
}: {
  field: FieldLike
  label: string
  type?: string
  autoComplete?: string
  placeholder?: string
}) {
  const errors = field.state.meta.errors

  return (
    <label className="flex flex-col gap-1.5 text-sm font-semibold text-[var(--sea-ink)]">
      {label}
      <input
        className="demo-input"
        type={type}
        autoComplete={autoComplete}
        placeholder={placeholder}
        value={field.state.value}
        onBlur={field.handleBlur}
        onChange={(e) => field.handleChange(e.target.value)}
      />
      {errors.length > 0 && (
        <span className="text-xs font-medium text-[#9f3030]">
          {errors
            .map((e) =>
              typeof e === 'string'
                ? e
                : ((e as { message?: string }).message ?? String(e)),
            )
            .join(', ')}
        </span>
      )}
    </label>
  )
}
