import { useForm } from '@tanstack/react-form'
import { useNavigate } from '@tanstack/react-router'
import { useState } from 'react'
import { api } from '#/api/client'
import { setSession } from '../../session'
import { loginSchema } from '../schema'

export function useLogin() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  const form = useForm({
    defaultValues: { email: '', password: '' },
    validators: { onSubmit: loginSchema },
    onSubmit: async ({ value }) => {
      setError(null)
      try {
        const data = await api.post('/auth/login', value)
        setSession({ token: data.token.value, user: data.user })
        navigate({ to: '/' })
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Login failed')
      }
    },
  })

  return { form, error }
}
