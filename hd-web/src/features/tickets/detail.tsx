import { Link, Navigate, getRouteApi } from '@tanstack/react-router'
import { useEffect, useState } from 'react'
import { useSession } from '../auth/session'
import { useTicket } from './hooks/useTicket'
import { CommentForm } from './components/CommentForm'
import { TicketControls } from './components/TicketControls'

const routeApi = getRouteApi('/tickets/$ticketId')

export function TicketDetailPage() {
  const session = useSession((s) => s.session)
  const [mounted, setMounted] = useState(false)
  useEffect(() => setMounted(true), [])

  const { ticketId } = routeApi.useParams()
  const id = Number(ticketId)
  const { ticket, error, setTicket } = useTicket(mounted && !!session && id > 0, id)

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
        <p className="mb-4">
          <Link to="/tickets" className="demo-muted text-sm">
            ← Tickets
          </Link>
        </p>
        {error && (
          <p className="demo-alert demo-alert-danger m-0">{error}</p>
        )}
        {!error && ticket === null && (
          <p className="demo-muted m-0">Loading…</p>
        )}
        {ticket && (
          <>
            <p className="island-kicker mb-2">Ticket #{ticket.id}</p>
            <h1 className="demo-title mb-4">{ticket.title}</h1>
            <div className="mb-6 flex flex-wrap gap-2">
              <span className="demo-pill">{ticket.status}</span>
              <span className="demo-pill">{ticket.category}</span>
              <span className="demo-pill">{ticket.priority}</span>
              <span className="demo-pill">
                {ticket.assigneeId === null
                  ? 'Unassigned'
                  : `Assignee #${ticket.assigneeId}`}
              </span>
            </div>
            <p className="demo-muted mb-6 text-xs">
              {ticket.requesterName} · Created{' '}
              {new Date(ticket.createdAt).toLocaleString()} · Updated{' '}
              {new Date(ticket.updatedAt).toLocaleString()}
            </p>
            <p className="whitespace-pre-wrap">{ticket.description}</p>

            <TicketControls
              ticket={ticket}
              user={session.user}
              onUpdated={(updated) =>
                setTicket((t) => (t ? { ...t, ...updated } : t))
              }
            />

            <h2 className="demo-muted mt-8 mb-3 text-sm font-semibold uppercase tracking-wider">
              Comments
            </h2>
            {ticket.comments.length === 0 ? (
              <p className="demo-muted m-0">No comments yet.</p>
            ) : (
              <ul className="m-0 list-none space-y-3 p-0">
                {ticket.comments.map((c) => (
                  <li
                    key={c.id}
                    className="rounded-xl border border-(--line) p-3"
                  >
                    <p className="demo-muted mb-1 text-xs">
                      #{c.authorId} · {new Date(c.createdAt).toLocaleString()}
                    </p>
                    <p className="m-0 whitespace-pre-wrap">{c.body}</p>
                  </li>
                ))}
              </ul>
            )}
            <CommentForm
              ticketId={ticket.id}
              onAdded={(comment) =>
                setTicket((t) =>
                  t ? { ...t, comments: [...t.comments, comment] } : t,
                )
              }
            />
          </>
        )}
      </section>
    </main>
  )
}
