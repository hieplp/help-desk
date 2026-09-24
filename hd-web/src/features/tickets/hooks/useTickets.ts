import { useEffect, useState } from 'react'
import { api } from '#/api/client'
import type { Status, TicketListItem } from '../types'

export function useTickets(enabled: boolean, status: Status | undefined) {
  const [tickets, setTickets] = useState<TicketListItem[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!enabled) return
    let cancelled = false
    setTickets(null)
    setError(null)
    api
      .get(status ? `/tickets?status=${status}` : '/tickets')
      .then((data) => {
        if (!cancelled) setTickets(data)
      })
      .catch((err) => {
        if (!cancelled)
          setError(err instanceof Error ? err.message : 'Failed to load tickets')
      })
    return () => {
      cancelled = true
    }
  }, [enabled, status])

  return { tickets, error }
}
