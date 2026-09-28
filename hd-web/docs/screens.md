# Screens

UI spec per route. Behavior contract: `../../docs/features.md`, `../../docs/api-rules.md`,
`../../docs/rules/validation-rules.md`. Structure rules: `project-structure.md`. What persists:
`state.md`.

Design language lives in `src/styles.css`: `demo-*` classes (`demo-page`, `demo-panel`, `demo-pill`,
`demo-button`, `demo-alert`, `island-kicker`, `nav-pill`) over Tailwind. Reuse them; no per-screen CSS.

## Global

- **Header** (all pages): logo → `/`; when logged in: `Tickets`, `New ticket` (desktop only),
  `Users` (agent only), user pill `name · role`, `Log out`; when logged out: `Log in`. Theme toggle
  always present.
- **Guard**: every protected page renders a `Loading…` shell until mounted, then `<Navigate
  to="/login">` when there is no session. The server still enforces — the guard is UX.
- **States every data page needs**: loading (`Loading…`), error (`demo-alert-danger` with the server
  message), empty, populated.
- Logout: `setSession(null)` → navigate to `/login`.

## `/` — home

Entry page, not a dashboard. Signed-out: "Welcome", `Sign in` → `/login`. Signed-in: greeting with
user name + role, `New ticket` → `/tickets/new`.

## `/login` — sign in

Public. Redirects to `/` when a session already exists. Shows the seeded demo accounts table.

- Fields: `email`, `password`. Submit → `POST /auth/login`.
- Success: `setSession` → `/`.
- Failure: `401` shows the server message in the form; the API never says which field failed.

## `/tickets` — ticket list

Protected. `GET /tickets?status=`.

- Table: title (→ detail), status, priority, assignee, updated.
- `status` filter is a select bound to the `?status=` search param; empty value = all.
- Requester sees own tickets only; agent sees all — the server decides, the UI renders the same page.
- Newest activity first (server order).

## `/tickets/new` — create ticket

Protected. `POST /tickets`.

- Fields: `title` (≤120), `description` (≤4000), `category`, `priority` — zod schema mirrors the
  validation rules; client validation is a convenience, never the enforcement.
- Success → `/` (home). The new ticket lands `open`, unassigned, and appears in `/tickets`.
- No status or assignee input — the caller cannot set either.

## `/tickets/$ticketId` — ticket detail

Protected. `GET /tickets/{id}`.

- Shows id, title, status/category/priority/assignee pills, requester, created/updated, description,
  comment thread (oldest first).
- `404` (foreign ticket or missing) renders as the error state — never distinguish "missing" from
  "not yours".
- **Planned** (spec'd, not built): agent status/assignee controls (`PATCH`), requester close action,
  comment form (`POST /tickets/{id}/comments`). Contract: `api/tickets.md`.

## `/users` — user list

Agent only. `GET /users`. Requesters are redirected to `/`; the server answers `403` regardless.
Table: name, email, role. Exists to feed the future assign dropdown.

## Navigation map

```
/ ── signed out ──▶ /login ── success ──▶ /
/ ── signed in ───▶ /tickets/new, /tickets
/tickets ── row click ──▶ /tickets/$ticketId ── back ──▶ /tickets
header: Tickets · New ticket · Users (agent) · Log out
```
