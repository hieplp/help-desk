# Users

`api.get('/users')` — agent only; the requester never calls it (403). Used by the `/users` page and, later, the ticket assign dropdown. Contract: `../../hd-service/docs/api/users.md`.

Passwords never leave the server.
