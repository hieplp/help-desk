# API client

How the frontend calls hd-service. Conventions, status, and contracts live in `../../hd-service/docs/api/*.md` and `../../docs/api-rules.md`. Do not restate them here.

## Client

`src/api/client.ts` exports `api` with `get`, `post`, `put`, `patch`, `delete` (put exists in wrapper but service api-rules: PUT is not used).

- All calls go through `api.*`. Never use raw `fetch`.
- Base path `/api` in the browser; `vite.config.ts` proxies to `http://localhost:8080` and strips the prefix in dev.
- Sends `Authorization: Bearer <token>` when `getSession()` returns a value.
- JSON only: sets `Content-Type: application/json` on body, stringifies, parses response.
- `!res.ok` → `throw new Error(data?.message ?? \`Request failed (${res.status})\`)`.
- `204` → `null`; else `res.json()`.

## Rules

- Caller identity is the token. Never send `userId`, `requesterId`, etc. from the client; the server derives from JWT.
- Branch on HTTP status or the error `code` (when present), never on `message` text.
- The API contract is in `../../hd-service/docs/api/*.md`. Update the spec first, then the client call.
- Errors surface as thrown `Error` with the server message or a fallback. UI catches and shows.

## Not used

- No per-call interceptors beyond the session header.
- No automatic retry.
- No query-string helpers beyond what the caller builds.