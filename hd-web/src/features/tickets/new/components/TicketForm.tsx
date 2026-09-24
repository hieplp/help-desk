import { FormField } from '#/components/fields/FormField'
import { useCreateTicket } from '../hooks/useCreateTicket'

const CATEGORIES = ['hardware', 'software', 'access', 'other']
const PRIORITIES = ['low', 'medium', 'high']

export function TicketForm() {
  const { form, error } = useCreateTicket()

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        form.handleSubmit()
      }}
      className="flex flex-col gap-4"
    >
      <form.Field name="title">
        {(field) => (
          <FormField
            field={field}
            label="Title"
            placeholder="Laptop will not boot"
          />
        )}
      </form.Field>
      <form.Field name="description">
        {(field) => (
          <FormField
            field={field}
            label="Description"
            rows={5}
            placeholder="What happened? What did you try?"
          />
        )}
      </form.Field>
      <div className="grid grid-cols-2 gap-4">
        <form.Field name="category">
          {(field) => (
            <FormField field={field} label="Category" options={CATEGORIES} />
          )}
        </form.Field>
        <form.Field name="priority">
          {(field) => (
            <FormField field={field} label="Priority" options={PRIORITIES} />
          )}
        </form.Field>
      </div>
      {error && <p className="demo-alert demo-alert-danger m-0">{error}</p>}
      <form.Subscribe selector={(state) => state.isSubmitting}>
        {(isSubmitting) => (
          <button className="demo-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Creating…' : 'Create ticket'}
          </button>
        )}
      </form.Subscribe>
    </form>
  )
}
