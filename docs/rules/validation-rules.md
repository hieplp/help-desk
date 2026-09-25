# Help desk — validation rules

Server-side input validation. Matches `docs/api-rules.md`. Client validation is a convenience, never a substitute.

## General

- All string input is trimmed before checks.
- Required means: present, non-null, non-empty after trim.
- Unknown fields in a request body are rejected → `400`.
- Ids are integers. Non-integer or negative → `400`.
- Enum values must match exactly, lowercase. Anything else → `400`.
- Every failure → `400` with `{ "code": "bad_request", "message": "..." }`. One message, first failure wins.

## Field limits

| Field | Rules |
|---|---|
| `email` | Required, valid email shape, max 254. Case-insensitive match. |
| `password` | Required, min 8, max 72 (bcrypt limit). |
| `name` | Required, max 120. |
| `title` | Required, max 120. |
| `description` | Required, max 4000. |
| `body` (comment) | Required, max 2000. |
| `status`, `priority`, `category`, `role` | Known enum value only. |
| `assigneeId` | Integer or `null`. Must reference an agent. |

## Endpoint specifics

- `POST /auth/login`: `email` + `password` required. Failure → `401`, never `400`, same message either way.
- `POST /tickets`: caller cannot send `status`, `requesterId`, or `assigneeId` → `400` if present.
- `PATCH /tickets/:id`: `status` required, known enum value only; other fields rejected. Field-level `400` before role-level `403`.
- `PATCH /tickets/:id/assignee`: `assigneeId` required key, integer or `null`, must reference an agent. Field-level `400` before role-level `403`.
- `GET /tickets?status=`: unknown status → `400`.
- `POST /tickets/:id/comments`: `body` required. Closed ticket still accepts comments.

## What is not validated

- No HTML sanitization — output is JSON, the client escapes on render.
- No rate limiting, no password strength meter. Add when abuse shows up.
