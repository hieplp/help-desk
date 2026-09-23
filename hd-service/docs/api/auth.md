# Auth

Conventions, error body, and status codes: `../api-rules.md`. Token rules: `../security-rules.md`.

## `POST /auth/login`

Public. The only public endpoint besides the OpenAPI/Swagger UI docs.

Request:

```json
{
  "email": "a@b.co",
  "password": "secret"
}
```

`200`:

```json
{
  "token": {
    "value": "<jwt>",
    "expiresAt": "2026-09-22T18:00:00Z"
  },
  "user": {
    "id": 1,
    "name": "Ada",
    "email": "a@b.co",
    "role": "agent"
  }
}
```

| Status | When                                                                                                                                    |
|--------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `400`  | Missing/blank `email` or `password`; bad email shape; email over 254 chars; password over 72 UTF-8 bytes; unknown fields; non-JSON body |
| `401`  | Unknown email or wrong password — one message, never says which                                                                         |

`email` is trimmed and lowercased before the lookup — `A@B.co` logs in as `a@b.co`. `password` is trimmed; no minimum
length on login. The 72-byte cap is UTF-8 bytes, not chars (bcrypt truncates there).

Seeded accounts: `a@b.co` / `secret` (agent), `b@b.co` / `secret` (requester).

No logout endpoint. The client drops the token.
