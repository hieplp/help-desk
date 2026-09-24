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
  options,
  rows,
}: {
  field: FieldLike
  label: string
  type?: string
  autoComplete?: string
  placeholder?: string
  options?: readonly string[]
  rows?: number
}) {
  const errors = field.state.meta.errors

  return (
    <label className="flex flex-col gap-1.5 text-sm font-semibold text-[var(--sea-ink)]">
      {label}
      {options ? (
        <select
          className="demo-input"
          value={field.state.value}
          onBlur={field.handleBlur}
          onChange={(e) => field.handleChange(e.target.value)}
        >
          {options.map((option) => (
            <option key={option} value={option}>
              {option.replace('_', ' ')}
            </option>
          ))}
        </select>
      ) : rows ? (
        <textarea
          className="demo-input"
          rows={rows}
          placeholder={placeholder}
          value={field.state.value}
          onBlur={field.handleBlur}
          onChange={(e) => field.handleChange(e.target.value)}
        />
      ) : (
        <input
          className="demo-input"
          type={type}
          autoComplete={autoComplete}
          placeholder={placeholder}
          value={field.state.value}
          onBlur={field.handleBlur}
          onChange={(e) => field.handleChange(e.target.value)}
        />
      )}
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
