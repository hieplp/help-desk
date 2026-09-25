import { useState } from 'react'
import { api } from '#/api/client'
import type { TicketResponse } from '../types'

export function usePatchTicket(id: number) {
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  const patch = async (body: { status?: string; assigneeId?: number | null }) => {
    setError(null)
    setPending(true)
    try {
      return (await api.patch(`/tickets/${id}`, body)) as TicketResponse
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update ticket')
      return null
    } finally {
      setPending(false)
    }
  }

  return { patch, error, pending }
}
