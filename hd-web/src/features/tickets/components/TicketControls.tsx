import { toast } from '#/components/toast'
import { useUsers } from '#/features/users/hooks/useUsers'
import type { Session } from '#/features/auth/session'
import { usePatchTicket } from '../hooks/usePatchTicket'
import type { Status, TicketDetail, TicketResponse } from '../types'

const AGENT_STATUSES: { value: Status; label: string }[] = [
  { value: 'in_progress', label: 'In progress' },
  { value: 'resolved', label: 'Resolved' },
  { value: 'closed', label: 'Closed' },
]

export function TicketControls({
  ticket,
  user,
  onUpdated,
}: {
  ticket: TicketDetail
  user: Session['user']
  onUpdated: (updated: TicketResponse) => void
}) {
  const { patch, error, pending } = usePatchTicket(ticket.id)
  const isAgent = user.role === 'agent'
  // Requesters never call GET /users — it 403s for them.
  const { users } = useUsers(isAgent && ticket.status !== 'closed')

  if (ticket.status === 'closed') return null

  const apply = async (body: { status?: string; assigneeId?: number | null }) => {
    const label =
      body.status !== undefined
        ? `Set status to ${body.status.replace('_', ' ')}?`
        : body.assigneeId === null
          ? 'Unassign this ticket?'
          : 'Change assignee?'
    if (!window.confirm(label)) return
    const updated = await patch(body)
    if (updated) {
      toast('Ticket updated')
      onUpdated(updated)
    }
  }

  const isOwn = ticket.requesterId === user.id
  if (!isAgent && !isOwn) return null

  return (
    <div className="mt-6 flex flex-col gap-3">
      {isAgent && (
        <>
          <div className="flex flex-wrap gap-2">
            {AGENT_STATUSES.map((s) => (
              <button
                key={s.value}
                type="button"
                className={
                  s.value === 'closed'
                    ? 'demo-button demo-button-danger'
                    : 'demo-button'
                }
                disabled={pending || ticket.status === s.value}
                onClick={() => apply({ status: s.value })}
              >
                {s.label}
              </button>
            ))}
          </div>
          <label className="flex flex-col gap-1.5 text-sm font-semibold text-[var(--sea-ink)]">
            Assignee
            <select
              className="demo-input max-w-xs"
              disabled={pending || users === null}
              value={ticket.assigneeId ?? ''}
              onChange={(e) =>
                apply({
                  assigneeId: e.target.value === '' ? null : Number(e.target.value),
                })
              }
            >
              <option value="">Unassigned</option>
              {(users ?? [])
                .filter((u) => u.role === 'agent')
                .map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.name} ({u.email})
                  </option>
                ))}
            </select>
          </label>
        </>
      )}
      {!isAgent && isOwn && (
        <div>
          <button
            type="button"
            className="demo-button demo-button-danger"
            disabled={pending}
            onClick={() => apply({ status: 'closed' })}
          >
            Close ticket
          </button>
        </div>
      )}
      {error && <p className="demo-alert demo-alert-danger m-0">{error}</p>}
    </div>
  )
}
