# Auth

Conventions, error body, and status codes: `../api-rules.md`. Token rules: `../security-rules.md`.

## `POST /auth/login`

Public. The only public endpoint.

Request:

```json
{ "email": "a@b.co", "password": "secret" }
```

`200`:

```json
{
  "token": { "value": "<jwt>", "expiresAt": "2026-09-22T18:00:00Z" },
  "user": { "id": 1, "name": "Ada", "email": "a@b.co", "role": "agent" }
}
```

Errors:

| Status | When |
| --- | --- |
| `400` | Missing or blank `email` / `password` |
| `401` | Unknown email or wrong password — one message, never says which |

No logout endpoint. The client drops the token.
