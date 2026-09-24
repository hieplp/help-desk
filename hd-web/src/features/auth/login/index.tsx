import { Navigate } from '@tanstack/react-router'
import { useSession } from '../session'
import { LoginForm } from './components/LoginForm'

export function LoginPage() {
  const session = useSession((s) => s.session)

  if (session) return <Navigate to="/" />

  return (
    <main className="demo-page demo-center">
      <section className="demo-panel rise-in w-full max-w-md">
        <p className="island-kicker mb-2">Help Desk</p>
        <h1 className="demo-title mb-6">Sign in</h1>
        <LoginForm />
        <div className="mt-6 rounded-xl border border-(--line) p-4 text-sm">
          <p className="island-kicker mb-2">Demo accounts</p>
          <table className="w-full text-left">
            <thead className="demo-muted text-xs">
              <tr>
                <th className="pb-1 font-semibold">Email</th>
                <th className="pb-1 font-semibold">Name</th>
                <th className="pb-1 font-semibold">Role</th>
                <th className="pb-1 font-semibold">Password</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>agent@b.co</td>
                <td>Agent</td>
                <td>agent</td>
                <td>secret</td>
              </tr>
              <tr>
                <td>requester@b.co</td>
                <td>Requester</td>
                <td>requester</td>
                <td>secret</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  )
}
