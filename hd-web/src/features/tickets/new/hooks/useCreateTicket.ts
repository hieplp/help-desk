import { useForm } from '@tanstack/react-form'
import { useNavigate } from '@tanstack/react-router'
import { useState } from 'react'
import { api } from '#/api/client'
import { toast } from '#/components/toast'
import { createTicketSchema } from '../schema'

export function useCreateTicket() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  const form = useForm({
    defaultValues: {
      title: '',
      description: '',
      category: 'hardware',
      priority: 'medium',
    },
    validators: { onSubmit: createTicketSchema },
    onSubmit: async ({ value }) => {
      if (!window.confirm('Create this ticket?')) return
      setError(null)
      try {
        await api.post('/tickets', value)
        toast('Ticket created')
        navigate({ to: '/' })
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to create ticket')
      }
    },
  })

  return { form, error }
}
