# Security rules

Every endpoint follows these rules. HTTP status and the error body stay in `api-rules.md`. This file is how authentication is built, and what must not leak.

Code lives in `config`. Controllers do not read the `Authorization` header.

## Authentication

- Stateless JWT. No server session, no auth cookie, no refresh token.
- A protected call sends `Authorization: Bearer <token>`.
- Missing, expired, malformed, or wrongly signed token is `401`.
- Logout is the client discarding the token. No logout endpoint until a token must be revoked.
- Login is the only public write. Every other endpoint is protected unless this file lists it as public.
- A failed login is one `401`. Same message for an unknown account and a wrong password.
- The caller is the token subject. Never accept a caller id from the body or the query.

## Token

- Sign with one algorithm only. Reject every other algorithm, including `none`.
- Claims are `sub` (user id), `role`, and `exp`. Nothing else.
- Expiry is configured. Default is 8 hours.
- The signing secret comes from the environment. It is not in git, not in `application.yaml`, and not in logs.
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
- Use `404`, not `403`, when `403` would confirm the id exists. `403` is for an authenticated caller who may know the resource and still may not act.

## Browser and transport

- Auth is the Bearer header. CSRF stays off because there is no cookie session.
- Allow only configured origins. No `*` origin.
- HTTPS outside local. Local HTTP is the exception.

## Failures

- Do not log the `Authorization` header, the token, or the password.
- A client error has no stack trace, SQL, or internal class name.
- Do not add a second security mechanism (API keys, sessions, OAuth) beside this one.
