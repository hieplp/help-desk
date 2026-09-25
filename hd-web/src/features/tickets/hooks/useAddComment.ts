import { useState } from 'react'
import { api } from '#/api/client'
import type { Comment } from '../types'

export function useAddComment(ticketId: number) {
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  const add = async (body: string) => {
    setError(null)
    setPending(true)
    try {
      return (await api.post(`/tickets/${ticketId}/comments`, { body })) as Comment
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to post comment')
      return null
    } finally {
      setPending(false)
    }
  }

  return { add, error, pending }
}
