# Security rules

Client-side rules. Server rules and error shapes are in `../../hd-service/docs/security-rules.md` and `../../hd-service/docs/api-rules.md`. Protected UX hides or redirects; the server is the enforcement point.

## Token

- Stored only in the zustand store persisted to `localStorage` key `hd.session`.
- Sent exclusively as `Authorization: Bearer <token>` header (see `api/client.ts`).
- Never appears in a URL, query param, route state, log, or error body.
- Logout is `setSession(null)`. No logout endpoint call.

## Storage

- `localStorage` only for `hd.session` (token + user) and `theme` ('light'|'dark'|'auto').
- The theme script in `src/routes/__root.tsx` runs before paint via `dangerouslySetInnerHTML` (static string only).
- No sessionStorage, no cookies for auth.

## XSS and rendering

- React escapes all text content. Never use `dangerouslySetInnerHTML` with user-controlled data.
- The single `dangerouslySetInnerHTML` usage is the hardcoded `THEME_INIT_SCRIPT` in `__root.tsx`.
- User data (name, email, role, ticket fields) always goes through text nodes or safe props.

## Routes and UX

- No token in route params, search params, or hash.
- Login page redirects to `/` if session exists (`Navigate`).
- Header shows user pill and logout only when session present.
- A protected feature must check session itself; absence of a token in the client is not a security guarantee.

## Failures

- The client does not auto-clear the session on `401` (the server may still consider the token valid; a stale token just keeps failing).
- The only session clear is explicit `setSession(null)` logout.
- Never log the token.