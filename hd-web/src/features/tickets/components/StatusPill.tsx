import type { Status } from '../types'

export function StatusPill({ status }: { status: Status }) {
  return <span className={`demo-pill status-${status}`}>{status}</span>
}
