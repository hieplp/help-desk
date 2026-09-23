# Tickets

Not implemented yet — this is the spec. Conventions, error body, and status codes: `../api-rules.md`.

Ticket fields: `id`, `title`, `description`, `category`, `priority`, `status`, `requesterId`, `assigneeId` (nullable),
`createdAt`, `updatedAt`.

Enums: `category` = `hardware` `software` `access` `other`; `priority` = `low` `medium` `high`; `status` = `open`
`in_progress` `resolved` `closed`.

List items omit `description`. Detail adds `description` and `comments`.

## `POST /tickets`

Any authenticated caller. `requesterId` is the token subject — never taken from the body. `status` starts `open`,
`assigneeId` starts `null`; the caller cannot set either.

```json
{
  "title": "Laptop will not boot",
  "description": "Black screen after the logo.",
  "category": "hardware",
  "priority": "high"
}
```

`201`: the created ticket.

Validation: `title` and `description` required, trimmed, non-empty; title max 120, description max 4000. `category` and
`priority` must be known values.

## `GET /tickets?status=`

- Requester: only own tickets (`requesterId` = caller).
- Agent: all tickets.
- `status` optional; unknown value → `400`.

`200`: array of list items, newest `updatedAt` first.

## `GET /tickets/{id}`

- Requester: own ticket only; someone else's → `404`.
- Agent: any ticket.

`200`: ticket plus `comments`, oldest first.

## `PATCH /tickets/{id}`

Partial. Only `status` and `assigneeId` are accepted. Empty body → `400`.

```json
{ "status": "in_progress", "assigneeId": 2 }
```

- `assigneeId: null` unassigns. Assignee must be an agent → else `400`.
- Agent may set `in_progress`, `resolved`, or `closed` from any non-closed status.
- Requester may set only `closed`, and only on their own ticket.
- `closed` is terminal. No reopen.
- Any other transition → `403`. Another requester's ticket → `404`.

`200`: the updated ticket, no comments.

## `POST /tickets/{id}/comments`

Append a comment. Requester: own ticket only, else `404`. Agent: any ticket. Closed tickets still accept comments.

```json
{ "body": "Tried a different charger." }
```

`201`:

```json
{
  "id": 9,
  "ticketId": 3,
  "authorId": 1,
  "body": "Tried a different charger.",
  "createdAt": "2026-09-22T10:00:00Z"
}
```

Validation: `body` required, trimmed, non-empty, max 2000.

No edit, no delete, no list endpoint — comments are read only via ticket detail.
