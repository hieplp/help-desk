import { Navigate } from '@tanstack/react-router'
import { useEffect, useState } from 'react'
import { useSession } from '../auth/session'
import { useUsers } from './hooks/useUsers'

export function UsersPage() {
  const session = useSession((s) => s.session)
  // SSR has no localStorage — session is null there and Navigate would redirect
  // server-side. Render a shell until mounted, then guard on the client.
  const [mounted, setMounted] = useState(false)
  useEffect(() => setMounted(true), [])
  const isAgent = session?.user.role === 'agent'
  const { users, error } = useUsers(mounted && isAgent)

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
  if (!isAgent) return <Navigate to="/" />

  return (
    <main className="demo-page">
      <section className="demo-panel rise-in">
        <p className="island-kicker mb-2">Help Desk</p>
        <h1 className="demo-title mb-6">Users</h1>
        {error && <p className="demo-alert demo-alert-danger m-0">{error}</p>}
        {users === null && <p className="demo-muted m-0">Loading…</p>}
        {users && (
          <table className="w-full text-left text-sm">
            <thead className="demo-muted text-xs">
              <tr>
                <th className="pb-2 font-semibold">Name</th>
                <th className="pb-2 font-semibold">Email</th>
                <th className="pb-2 font-semibold">Role</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id} className="border-t border-(--line)">
                  <td className="py-2.5 font-semibold">{user.name}</td>
                  <td className="py-2.5">{user.email}</td>
                  <td className="py-2.5">
                    <span className="demo-pill">{user.role}</span>
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
