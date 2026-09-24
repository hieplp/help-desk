import { Link, createFileRoute } from '@tanstack/react-router'
import { useSession } from '../features/auth/session'

export const Route = createFileRoute('/')({ component: Home })

function Home() {
  const session = useSession((s) => s.session)

  return (
    <main className="demo-page demo-center">
      <section className="demo-panel rise-in w-full max-w-md">
        <p className="island-kicker mb-2">Help Desk</p>
        <h1 className="demo-title mb-4">
          {session ? `Hi, ${session.user.name}` : 'Welcome'}
        </h1>
        <p className="demo-muted m-0">
          {session
            ? `Signed in as ${session.user.role}.`
            : 'Sign in to manage your tickets.'}
        </p>
        {!session && (
          <Link to="/login" className="demo-button mt-6 no-underline">
            Sign in
          </Link>
        )}
      </section>
    </main>
  )
}
