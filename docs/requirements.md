# Help desk — requirements

Business requirements. `features.md` is the scope contract, `api-rules.md` the API contract.
This doc says *why* and *who*; those say *what*. Conflicts → fix the doc, then the code.

## Problem

Internal IT support runs over chat and hallway requests. Tickets get lost, nobody knows
who owns what, requesters can't see progress. One shared system: requesters file tickets,
agents triage and resolve them.

## Goals

- Every support request becomes a tracked ticket with one owner-visible status.
- Requesters see their own tickets and their full history.
- Agents see everything, assign work, move status.
- Small enough to build and run without a support team of its own.

Non-goals: the `features.md` out-of-scope list is binding — no attachments, email, SLA,
search, dashboard, knowledge base, audit log, password reset.

## Actors

| Actor | Who | Access |
|---|---|---|
| Requester | Any employee with a seeded account | Own tickets only |
| Agent | IT support staff | All tickets, assignment, status |

Agents can also file tickets — an agent is a requester for their own tickets.
No admin role, no self-registration. Accounts are seeded by whoever deploys it.

## User stories

### Requester

- R1: Log in with email + password → get a token for the session.
- R2: File a ticket with title, description, category, priority → it lands `open`, unassigned.
- R3: See my tickets, newest activity first; filter by status.
- R4: Open a ticket → see all fields and the full comment thread.
- R5: Add a comment to my ticket, including after it's closed.
- R6: Close my own ticket when I'm satisfied — even if the agent never resolved it.
- R7: I cannot see, guess, or probe other people's tickets — a foreign ticket id looks like it doesn't exist.

### Agent

- A1: See every ticket, newest activity first; filter by status.
- A2: Assign a ticket to any agent, or unassign it.
- A3: Move a ticket `open` → `in_progress` → `resolved` → `closed`. Skipping steps is allowed.
- A4: Comment on any ticket.
- A5: List users to pick an assignee.

## Business rules

### Permission matrix

| Action | Requester | Agent |
|---|---|---|
| Create ticket | ✓ (own) | ✓ (own) |
| List tickets | Own only | All |
| View ticket | Own only, else `404` | All |
| Set status | `closed` only, own ticket | Any non-closed → any status |
| Assign / unassign | — | ✓, assignee must be an agent |
| Comment | Own tickets | All tickets |
| List users | — | ✓ |

### Status lifecycle

`open` → `in_progress` → `resolved` → `closed`.

- `closed` is terminal. No reopen — a recurring problem is a new ticket.
- Agents may jump straight to `closed`; `resolved` is a courtesy signal, not a gate.
- Comments stay open on closed tickets — the record can still be annotated.

### Data rules

- Tickets are never deleted. Comments are never edited or deleted — the thread is the record.
- `requesterId` is always the caller; it can never be set or changed.
- Unassigned is a valid state — triage may lag filing.
- Login failure never reveals whether the email or the password was wrong.

## Acceptance criteria

- A requester filing a ticket sees it in their list immediately, status `open`, no assignee.
- A requester hitting another user's ticket URL gets `404`, not `403` — existence is not leaked.
- An agent filtering by `in_progress` sees only `in_progress` tickets, across all requesters.
- Closing a ticket blocks all further status changes; comments still work.
- Assigning to a requester account is rejected.
- Token expiry forces re-login; there is no refresh path.

## Non-functional requirements

- Token lifetime 8 hours — one workday per login.
- Passwords hashed (bcrypt); plaintext never stored or returned.
- No pagination until a list is measurably slow — expected volume is small.
- JSON only; the web client is the only consumer.

## Assumptions

- Single deployment, single backend, one web client.
- User list is small and static — seeded accounts are enough.
- No notifications: requesters poll their ticket list for progress.
- English only.
