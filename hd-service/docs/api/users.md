# Users

Conventions, error body, and status codes: `../api-rules.md`.

## `GET /users`

Agent only. Feeds the assign dropdown. Passwords never leave the server.

`200`:

```json
[{ "id": 2, "name": "Bea", "email": "b@b.co", "role": "agent" }]
```

Errors:

| Status | When |
| --- | --- |
| `401` | No or bad token |
| `403` | Caller is a requester |

No other user endpoints. Accounts are seeded; there is no registration.
