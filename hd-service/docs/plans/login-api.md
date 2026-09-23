# Spec: login API

`POST /auth/login` in `hd-service`. Resource summary: `../api/auth.md`.

This file is what to build. It wins over:

- string `token` in `../../../docs/api-rules.md` — token is an object
- the login bullet in `../../../docs/rules/validation-rules.md` (`401`, never `400`) — bad input is `400`
- `{code, message, fields}` in `../api-rules.md` — body is `{ "error": "message" }`

As built, deviations from this spec: SQLite (`jdbc:sqlite:helpdesk.db`, `DB_URL` env) instead of Postgres; seeds are
`a@b.co` (Ada, agent) and `b@b.co` (Bea, requester); `JwtService`/`JwtAuthFilter` live in `security/`, not `config/`;
DTOs are one record per file under `model/dto/`; Swagger UI (`/v3/api-docs/**`, `/swagger-ui/**`) is also public.

## Contract

Only public route. A token sent to it is ignored. Every other route requires `Authorization: Bearer <jwt>`.

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

`expiresAt` is the `exp` claim. One clock read. UTC, second precision, `Z`, no fractional seconds.

## Errors

Always `{ "error": "message" }`. Not ProblemDetail, not Boot's `{timestamp, status, error, path}`. No stack, SQL, or
class name in the body. `500` is `{ "error": "Internal error" }`; the stack stays in the log.

| Status | When                                                                                                | `error`                     |
|--------|-----------------------------------------------------------------------------------------------------|-----------------------------|
| `400`  | Bad input, before any lookup                                                                        | `Request is invalid`        |
| `401`  | Unknown email or wrong password — identical body                                                    | `Invalid email or password` |
| `401`  | Any other route: missing, expired, malformed, or wrongly signed token — identical body, never `403` | `Unauthorized`              |

`400` order, first hit wins:

1. Missing body, non-object, malformed JSON, or unknown fields.
2. `email` null or blank after trim.
3. After trim and lowercase: not one `@` with a dot after it, or longer than 254.
4. `password` null or blank after trim.
5. `password` longer than 72 UTF-8 bytes (bcrypt truncates at 72 bytes — check bytes, not `length()`).

No minimum length on login. `"secret"` is valid input. A short wrong password is `401`. Non-JSON content type is the
same `400`. Do not add `415`.

## Wiring

- `spring-security-oauth2-jose` (Boot BOM, no version). HS256 through Spring `JwtEncoder` / `JwtDecoder`. Do not
  hand-roll HMAC. Do not add the OAuth2 resource-server starter — it is a second mechanism and wants a JWKS URI.
- `JWT_SECRET` from the environment. Required, ≥ 32 bytes, else refuse to start. Never in git, main `application.yaml`,
  or logs. TTL is `app.jwt.ttl`, default `8h`, in `application.yaml` (not a secret).
- Jackson fails on unknown properties. Boot's default is off, which would accept them.
- Postgres from env. Defaults: `jdbc:postgresql://localhost:5432/helpdesk`, user `helpdesk`, password `helpdesk`.
  `ddl-auto: update`. No Flyway, no Docker.
- Tests: `testRuntimeOnly` H2 (BOM, no version) and `src/test/resources/application.yaml` with a mem URL plus a test
  `JWT_SECRET` (≥ 32 bytes). `./gradlew build` must not need Postgres. Column stays `varchar(255)` per `../database.md`;
  the 254 cap is the service check.

## Code

Package `dev.hieplp.helpdesk`. Lombok on the entity. Ids are `Long`.

| Class                           | Job                                                                                                                                                                                                                                                                     |
|---------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `model/Role`                    | `requester`, `agent`. Stored as that lowercase name.                                                                                                                                                                                                                    |
| `model/User`                    | `id`, `name`, `email` (unique, always lowercase), `passwordHash`, `role`. Jackson and `toString` skip `passwordHash`. Never returned.                                                                                                                                   |
| `repository/UserRepository`     | `findByEmail`. Caller already lowercased. Exact match — not `IgnoreCase`.                                                                                                                                                                                               |
| `dto/LoginRequest`              | `email`, `password`.                                                                                                                                                                                                                                                    |
| `dto/LoginResponse`             | token `{value, expiresAt}` and user `{id, name, email, role}`. Nested records in this file. Only user shape.                                                                                                                                                            |
| `service/AuthService`           | `login`.                                                                                                                                                                                                                                                                |
| `service/AuthServiceImpl`       | Trim, then the `400` checks, then lookup. One `passwordEncoder.matches` on the trimmed password. No user → `matches` against a constant valid bcrypt hash, not one encoded per call. Either miss → the `401` above. Then `JwtService.issue`. No JWT code in this class. |
| `controller/AuthController`     | `POST /auth/login`. No repository, no header.                                                                                                                                                                                                                           |
| `config/JwtService`             | One class, no interface. Claims exactly `sub` (id as a string), `role`, `exp`. No `iat`, `iss`, or anything else. HS256 only; reject every other algorithm, including `none`.                                                                                           |
| `config/JwtAuthFilter`          | Read `Bearer`, `parse`. Principal is the user id (`Long`). Authority is the role name (`agent`), not `ROLE_agent` — later checks use `hasAuthority`, not `hasRole`.                                                                                                     |
| `config/SecurityConfig`         | `BCryptPasswordEncoder` bean. Stateless, no session, CSRF off, form login off, http basic off. Permit only `POST /auth/login`. Entry point writes the filter `401` body.                                                                                                |
| `config/SeedUsers`              | Insert if that email is absent. Store `passwordEncoder.encode` only. Do not log the password.                                                                                                                                                                           |
| `exception/ApiExceptionHandler` | Bad input → `400`, bad credentials → `401`. Body is `dto/ErrorResponse` (`code` + `message`). Filter `401` is the entry point, not this class.                                                                                                                          |

Seed (plaintext lives only here and in the seeder):

| email            | name      | role        | password |
|------------------|-----------|-------------|----------|
| `agent@b.co`     | Agent     | `agent`     | `secret` |
| `requester@b.co` | Requester | `requester` | `secret` |

Bea is the requester so both roles exist. `A@B.co` / `secret` is Ada. Response email is `a@b.co`.

## Do not add

Registration, password reset, refresh, logout, rate limit, CORS, Flyway, a users endpoint, a second security mechanism.
CORS arrives with the web app: one configured origin, never `*`.

## Verify

`./gradlew build` runs a MockMvc test (H2):

- `a@b.co` / `secret` and `A@B.co` / `secret` → `200`, `token.value` set, `expiresAt` equals `exp`, no `passwordHash`
- wrong password and unknown email → the same `401` body
- blank email → `400`; unknown field → `400`; a 73-byte password → `400`
- `GET /users` with no token → `401`; with Ada's token → not `401` (`404` is fine — do not implement users)

`bootRun` needs a local `helpdesk` database and a secret, then curl the same:

```bash
JWT_SECRET='dev-only-jwt-secret-must-be-32b!!' ./gradlew bootRun
```

Commit: `feat: add POST /auth/login endpoint`.
