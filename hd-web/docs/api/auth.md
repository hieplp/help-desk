# Auth

Thin client wrapper. Full contract, request/response shapes, status codes: `../../hd-service/docs/api/auth.md`.

## `POST /auth/login`

Via `api.post('/auth/login', { email, password })`.

On success: `setSession({ token: data.token.value, user: data.user })` then navigate to `/`.

Error: caught as `Error` with `data.message` or fallback; shown in login form.

Demo accounts shown on the login page:

| Email            | Name      | Role      | Password |
|------------------|-----------|-----------|----------|
| agent@b.co       | Agent     | agent     | secret   |
| requester@b.co   | Requester | requester | secret   |

No other auth calls. Logout is local `setSession(null)`.