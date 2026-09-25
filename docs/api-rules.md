# Help desk — API rules

JSON over HTTP. One backend. Matches `docs/features.md`. Nothing else.

## Conventions

- Auth header: `Authorization: Bearer <jwt>`. Missing or bad token → `401`.
- JWT claims: `sub` (user id), `role` (`requester` | `agent`), `exp`. Lifetime 8 hours. No refresh token.
- Logout has no endpoint. The client drops the token.
- Ids are integers.
- Error body is always `{ "error": "message" }`.
- `400` bad input, `403` wrong role or not your ticket, `404` missing.
- No pagination, no versioning, no query language. Add pagination when a list is actually slow.

## Enums

- `role`: `requester` | `agent`
- `status`: `open` | `in_progress` | `resolved` | `closed`
- `priority`: `low` | `medium` | `high`
- `category`: `hardware` | `software` | `access` | `other`

## Auth

### `POST /auth/login`

No token required.

```json
{ "email": "a@b.co", "password": "secret" }
```

`200`

```json
{
  "token": "<jwt>",
  "user": { "id": 1, "name": "Ada", "email": "a@b.co", "role": "agent" }
}
```

Unknown email or wrong password → `401` with the same message. Do not say which one failed.

## Users

### `GET /users`

Agent only. For the assign dropdown. Password never leaves the server.

`200`

```json
[{ "id": 2, "name": "Bea", "email": "b@b.co", "role": "agent" }]
```

## Tickets

Fields: `id`, `title`, `description`, `category`, `priority`, `status`, `requesterId`, `requesterName`, `assigneeId` (nullable), `assigneeName` (nullable), `createdAt`, `updatedAt`.

List items omit `description`. Detail includes `description` and `comments`.

### `POST /tickets`

Any logged-in user. `requesterId` is the caller. Status starts `open`. `assigneeId` starts `null`. Caller cannot set status or assignee here.

```json
{
  "title": "Laptop will not boot",
  "description": "Black screen after the logo.",
  "category": "hardware",
  "priority": "high"
}
```

`201` ticket object. Title and description required, trimmed, non-empty. Title max 120, description max 4000.

### `GET /tickets?status=`

`status` optional, must be a known status.

- Requester: only tickets where `requesterId` is the caller.
- Agent: all tickets.

`200` array of list items, newest `updatedAt` first.

### `GET /tickets/:id`

Requester: own ticket only, else `404` (do not confirm it exists). Agent: any ticket.

`200` ticket plus `comments`, oldest first.

### `PATCH /tickets/:id`

Partial. Only `status` and `assigneeId`. Empty body → `400`.

```json
{ "status": "in_progress", "assigneeId": 2 }
```

`assigneeId: null` unassigns. Assignee must be an agent, else `400`.

Status rules:

- Agent may set `in_progress`, `resolved`, or `closed` from any non-closed status.
- Requester may set only `closed`, and only on their own ticket.
- `closed` is terminal. No reopen.
- Anything else → `403`.

`200` updated ticket, no comments.

## Comments

### `POST /tickets/:id/comments`

Requester: own ticket only, else `404`. Agent: any ticket. Closed tickets still accept comments.

```json
{ "body": "Tried a different charger." }
```

`201`

```json
{
  "id": 9,
  "ticketId": 3,
  "authorId": 1,
  "authorName": "Ada",
  "body": "Tried a different charger.",
  "createdAt": "2026-09-22T10:00:00Z"
}
```

Body required, trimmed, non-empty, max 2000. No edit, no delete, no list endpoint. Comments are read only via ticket detail.
