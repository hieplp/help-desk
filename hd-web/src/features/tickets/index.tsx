import { Link, Navigate, getRouteApi, useNavigate } from '@tanstack/react-router'
import { useEffect, useState } from 'react'
import { useSession } from '../auth/session'
import { useTickets } from './hooks/useTickets'
import { STATUSES, type Status } from './types'
import { StatusPill } from './components/StatusPill'

const routeApi = getRouteApi('/tickets/')

export function TicketsPage() {
  const session = useSession((s) => s.session)
  const [mounted, setMounted] = useState(false)
  useEffect(() => setMounted(true), [])

  const { status } = routeApi.useSearch()
  const navigate = useNavigate()
  const { tickets, error } = useTickets(mounted && !!session, status)

  if (!mounted) {
    return (
      <main className="demo-page">
        <section className="demo-panel">
          <p className="demo-muted m-0">Loading…</p>
        </section>
      </main>
    )
  }
  if (!session) return <Navigate to="/login" />

  return (
    <main className="demo-page">
      <section className="demo-panel rise-in">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
          <div>
            <p className="island-kicker mb-2">Help Desk</p>
            <h1 className="demo-title">Tickets</h1>
          </div>
          <label className="demo-muted flex items-center gap-2 whitespace-nowrap text-sm">
            Status
            <select
              className="demo-input w-auto"
              value={status ?? ''}
              onChange={(e) =>
                navigate({
                  to: '/tickets',
                  search: {
                    status: (e.target.value || undefined) as Status | undefined,
                  },
                })
              }
            >
              <option value="">All</option>
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </label>
        </div>
        {error && <p className="demo-alert demo-alert-danger m-0">{error}</p>}
        {!error && tickets === null && (
          <p className="demo-muted m-0">Loading…</p>
        )}
        {tickets && tickets.length === 0 && (
          <p className="demo-muted m-0">No tickets.</p>
        )}
        {tickets && tickets.length > 0 && (
          <table className="w-full text-left text-sm">
            <thead className="demo-muted text-xs">
              <tr>
                <th className="pb-2 font-semibold">Title</th>
                <th className="pb-2 font-semibold">Category</th>
                <th className="pb-2 font-semibold">Priority</th>
                <th className="pb-2 font-semibold">Status</th>
                <th className="pb-2 font-semibold">Requester</th>
                <th className="pb-2 font-semibold">Assignee</th>
                <th className="pb-2 font-semibold">Updated</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map((t) => (
                <tr key={t.id} className="border-t border-(--line)">
                  <td className="py-2.5 font-semibold">
                    <Link
                      to="/tickets/$ticketId"
                      params={{ ticketId: String(t.id) }}
                      className="text-inherit"
                    >
                      {t.title}
                    </Link>
                  </td>
                  <td className="py-2.5">{t.category}</td>
                  <td className="py-2.5">{t.priority}</td>
                  <td className="py-2.5">
                    <StatusPill status={t.status} />
                  </td>
                  <td className="py-2.5">{t.requesterName}</td>
                  <td className="py-2.5">
                    {t.assigneeName ?? (
                      <span className="demo-muted">Unassigned</span>
                    )}
                  </td>
                  <td className="demo-muted py-2.5">
                    {new Date(t.updatedAt).toLocaleString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </main>
  )
}
