# Security rules

Every endpoint follows these rules. HTTP status and the error body stay in `api-rules.md`. This file is how
authentication is built, and what must not leak.

Code lives in `security/` (`JwtService`, `JwtAuthFilter`, `ApiAuthenticationEntryPoint`); wiring in
`config/SecurityConfig`. Controllers do not read the `Authorization` header.

## Authentication

- Stateless JWT. No server session, no auth cookie, no refresh token.
- A protected call sends `Authorization: Bearer <token>`.
- Missing, expired, malformed, or wrongly signed token is `401`.
- Logout is the client discarding the token. No logout endpoint until a token must be revoked.
- Login is the only public write. Public reads: `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`. Every other
  endpoint is protected.
- A failed login is one `401`. Same message for an unknown account and a wrong password.
- The caller is the token subject. Never accept a caller id from the body or the query.

## Current caller

- A controller reads the caller id with `@CurrentUser` on a `Long` parameter:

  ```java
  @PostMapping("/tickets")
  TicketResponse create(@CurrentUser Long userId, @Valid @RequestBody CreateTicketRequest body) { ... }
  ```

- `@CurrentUser` (in `security/`) wraps `@AuthenticationPrincipal`. The principal is the user id `JwtAuthFilter`
  sets from the token subject.
- Unauthenticated calls never reach the controller — the entry point answers `401` first, so the id is never null
  on a protected route.
- Services that need the caller id take it as a parameter from the controller. They do not read
  `SecurityContextHolder` themselves.

## Token

- HS256 only (`MacAlgorithm.HS256`). The decoder rejects every other algorithm, including `none`.
- Claims are `sub` (user id as a string), `role`, and `exp`. Nothing else.
- TTL is `app.jwt.ttl`, default `8h`, in `application.yaml` — not a secret.
- The signing secret is `JWT_SECRET` from the environment, minimum 32 bytes or the app refuses to start. It is not in
  git and not in logs. `application.yaml` carries a dev-only default; never commit a real one.
- Put the token in the login response body only. Never in a URL, a log line, or an error.

## Passwords

- Store a hash from Spring Security's password encoder. Never a plaintext password, never a custom hash.
- Compare only through that encoder.
- A response, a log line, and an error never include a password or a hash.
- Accounts are seeded. No public registration until that is an explicit feature.

## Authorization

- The filter authenticates. The service decides what that caller may do.
- A role check on the controller is not enough. The service checks again.
- Each endpoint states which roles may call it, and which rows those roles may see.
- A caller cannot widen that scope by passing another id.
- Use `404`, not `403`, when `403` would confirm the id exists. `403` is for an authenticated caller who may know the
  resource and still may not act.

## Browser and transport

- Auth is the Bearer header. CSRF stays off because there is no cookie session.
- Allow only configured origins. No `*` origin.
- HTTPS outside local. Local HTTP is the exception.

## Failures

- Do not log the `Authorization` header, the token, or the password.
- A client error has no stack trace, SQL, or internal class name.
- Do not add a second security mechanism (API keys, sessions, OAuth) beside this one.
