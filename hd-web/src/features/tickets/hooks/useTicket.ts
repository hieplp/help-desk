import { useEffect, useState } from 'react'
import { api } from '#/api/client'
import type { TicketDetail } from '../types'

export function useTicket(enabled: boolean, id: number) {
  const [ticket, setTicket] = useState<TicketDetail | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!enabled) return
    let cancelled = false
    api
      .get(`/tickets/${id}`)
      .then((data) => {
        if (!cancelled) setTicket(data)
      })
      .catch((err) => {
        if (!cancelled)
          setError(err instanceof Error ? err.message : 'Failed to load ticket')
      })
    return () => {
      cancelled = true
    }
  }, [enabled, id])

  return { ticket, error }
}
