# API rules

Every endpoint in hd-service follows these rules. A new endpoint does not invent a second style.

## Protocol

- JSON, UTF-8, `Content-Type: application/json`.
- No version prefix. Add a field or a resource before adding `/v2`.
- Paths are plural kebab-case nouns: `/resources`, `/resources/{id}`.
- Nest only when the child cannot exist without the parent: `/resources/{id}/children`.
- No verbs in paths. A state change is a field update, not a new URL.

## Methods

| Method | Use | Success |
| --- | --- | --- |
| `GET` | Read one resource or a collection. No body. No side effects. | `200` |
| `POST` | Create a resource. | `201`, body is the created resource |
| `PATCH` | Partial update. Absent field means unchanged. `null` clears a nullable field. | `200`, body is the updated resource |
| `DELETE` | Remove. | `204`, empty body |

- `PUT` is not used.
- An empty `PATCH` body is `400`.
- Unknown JSON fields are `400`.
- Strings are trimmed before validation. Blank after trim is missing.

## Auth

- Stateless JWT. The server stores no session.
- Protected calls send `Authorization: Bearer <token>`.
- A missing, expired, or invalid token is `401`.
- Authenticated but not allowed is `403`.
- The caller is the token subject. Never trust a caller id from the body or the query.
- An endpoint is protected unless it is explicitly public.
- Logout is the client discarding the token. Add a logout endpoint only when a token must be revoked.
- A failed credential check uses one `401` message. Do not reveal which part failed.
- A response never includes a password, a hash, or another caller's token.
- Browser access is limited to configured origins. Auth is the Bearer header, not a cookie.

## Status

- `400` malformed or invalid input
- `401` not authenticated
- `403` authenticated, not allowed
- `404` missing. Also `404` when the caller must not learn that the id exists
- `409` conflict with current state, including a unique constraint
- `415` body is not JSON
- `500` unexpected. No stack trace, no SQL, no internal class names

## Errors

Every failure uses this body. `fields` is present only for input errors.

```json
{
  "code": "validation_failed",
  "message": "Request is invalid",
  "fields": { "name": "must not be blank" }
}
```

- `code` is stable `lower_snake_case`. Clients branch on `code`, not on `message`.
- `message` is one short sentence.
- No per-endpoint error shape.

## JSON

- Property names are camelCase. Query parameter names match those properties.
- Enum values are `lower_snake_case` strings, never numbers.
- Every resource id is an integer.
- Timestamps are ISO-8601 UTC (`2026-09-22T10:00:00Z`). Persisted resources expose `createdAt` and `updatedAt`.
- One resource is a JSON object. No `{ "data": ... }` wrapper.
- Nullable fields are present in responses. `null` is a value, not an omission.

## Collections

- A collection is a JSON array.
- Default order is newest `updatedAt` first, unless that endpoint documents another order.
- A filter is a query parameter named exactly as the field. An unknown parameter is `400`.
- Sort, when an endpoint supports it, is one field: `sort=field` or `sort=-field`.
- Do not paginate until a collection is slow. The first paginated collection sets the shape, and every later one uses it: `page` (1-based), `size` (default 20, max 100).

```json
{ "items": [], "page": 1, "size": 20, "total": 0 }
```

## Authorization

- Each endpoint states which roles may call it, and which rows those roles may see.
- A caller cannot widen that scope by passing another id.
- Use `404`, not `403`, when `403` would confirm the id exists.

## Changes

- Add fields. Do not rename or remove a field unless the client changes in the same release.
- Do not add a second way to do the same thing.
