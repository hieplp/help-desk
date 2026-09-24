import { useLogin } from '../hooks/useLogin'
import { FormField } from '#/components/fields/FormField'

export function LoginForm() {
  const { form, error } = useLogin()

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        form.handleSubmit()
      }}
      className="flex flex-col gap-4"
    >
      <form.Field name="email">
        {(field) => (
          <FormField
            field={field}
            label="Email"
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
          />
        )}
      </form.Field>
      <form.Field name="password">
        {(field) => (
          <FormField
            field={field}
            label="Password"
            type="password"
            autoComplete="current-password"
            placeholder="Your password"
          />
        )}
      </form.Field>
      {error && <p className="demo-alert demo-alert-danger m-0">{error}</p>}
      <form.Subscribe selector={(state) => state.isSubmitting}>
        {(isSubmitting) => (
          <button className="demo-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Signing in…' : 'Sign in'}
          </button>
        )}
      </form.Subscribe>
    </form>
  )
}
