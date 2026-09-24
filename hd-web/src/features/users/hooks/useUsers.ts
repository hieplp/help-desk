import { useEffect, useState } from 'react'
import { api } from '#/api/client'

export type User = {
  id: number
  name: string
  email: string
  role: 'requester' | 'agent'
}

export function useUsers(enabled: boolean) {
  const [users, setUsers] = useState<User[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!enabled) return
    let cancelled = false
    api
      .get('/users')
      .then((data) => {
        if (!cancelled) setUsers(data)
      })
      .catch((err) => {
        if (!cancelled)
          setError(err instanceof Error ? err.message : 'Failed to load users')
      })
    return () => {
      cancelled = true
    }
  }, [enabled])

  return { users, error }
}
