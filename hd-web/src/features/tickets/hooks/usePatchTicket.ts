import { useState } from 'react'
import { api } from '#/api/client'
import type { TicketResponse } from '../types'

export function usePatchTicket(id: number) {
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  const run = async (path: string, body: unknown) => {
    setError(null)
    setPending(true)
    try {
      return (await api.patch(path, body)) as TicketResponse
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update ticket')
      return null
    } finally {
      setPending(false)
    }
  }

  return {
    updateStatus: (status: string) => run(`/tickets/${id}`, { status }),
    updateAssignee: (assigneeId: number | null) =>
      run(`/tickets/${id}/assignee`, { assigneeId }),
    error,
    pending,
  }
}
