import { Link, useNavigate } from '@tanstack/react-router'
import { setSession, useSession } from '../features/auth/session'
import ThemeToggle from './ThemeToggle'

export default function Header() {
  const session = useSession((s) => s.session)
  const navigate = useNavigate()
  return (
    <header className="sticky top-0 z-50 border-b border-[var(--line)] bg-[var(--header-bg)] px-4 backdrop-blur-lg">
      <nav className="page-wrap flex items-center gap-x-3 py-3 sm:py-4">
        <h2 className="m-0 flex-shrink-0 text-base font-semibold tracking-tight">
          <Link
            to="/"
            className="inline-flex items-center gap-2 rounded-full border border-[var(--chip-line)] bg-[var(--chip-bg)] px-3 py-1.5 text-sm text-[var(--sea-ink)] no-underline shadow-[0_8px_24px_rgba(30,90,72,0.08)] sm:px-4 sm:py-2"
          >
            <span className="h-2 w-2 rounded-full bg-[linear-gradient(90deg,#56c6be,#7ed3bf)]" />
            Help Desk
          </Link>
        </h2>

        <div className="ml-auto flex items-center gap-1.5 sm:gap-2">
          {session ? (
            <>
              <span className="demo-pill">
                {session.user.name} · {session.user.role}
              </span>
              <button
                type="button"
                className="nav-link cursor-pointer border-0 bg-transparent p-0 font-sans text-sm font-semibold"
                onClick={() => {
                  setSession(null)
                  navigate({ to: '/login' })
                }}
              >
                Log out
              </button>
            </>
          ) : (
            <Link
              to="/login"
              className="nav-link"
              activeProps={{ className: 'nav-link is-active' }}
            >
              Log in
            </Link>
          )}

          <ThemeToggle />
        </div>
      </nav>
    </header>
  )
}
