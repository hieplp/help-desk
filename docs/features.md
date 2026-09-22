# Help desk — core features

Two apps: web and backend. Two roles: **requester** and **agent**.

## Auth

- Login returns a JWT.
- Logout is the client dropping the token. No refresh tokens, no server session store.
- Users are seeded. No self-registration.

## Tickets

- Create: title, description, category, priority.
- List: requester sees their own; agent sees all. Filter by status.
- Detail: one ticket, its fields, its comments.
- Status: `open` → `in progress` → `resolved` → `closed`. Agent moves status. Requester can close their own.
- Assign: agent only. Unassigned is allowed.

## Comments

- Append a note on a ticket. Both roles. No edit, no delete.

## Out of scope

Attachments, email, SLA, search beyond the status filter, dashboard, knowledge base, audit log, password reset.
