# Help desk — architecture

System overview for engineers. Product spec: `requirements.md`, `features.md`. Contracts:
`api-rules.md`, `hd-service/docs/api-rules.md`. Per-app internals: `hd-service/AGENTS.md`,
`hd-web/AGENTS.md`.

## Shape

Two processes, one database file. The web app is the only API consumer.

```
browser ── :3000 ── hd-web (TanStack Start, Vite dev proxy /api → :8080)
                         │
                         ▼  JSON + Bearer JWT
                    hd-service ── :8080 ── SQLite file (helpdesk.db)
                    Spring Boot 4, JPA
```

- `hd-service` owns all rules: auth, roles, ticket scope, status transitions. Stateless — no server
  session; every protected call carries the JWT.
- `hd-web` renders and hides what a role must not see, but enforces nothing. The server rejects what
  the UI hides.
- `dev` proxy strips `/api`: browser calls `/api/tickets`, service sees `/tickets`.

## Request path

1. `hd-web` route renders a feature page; the page calls `api.*` (`hd-web/src/api/client.ts`).
2. Client attaches `Authorization: Bearer <token>` from the session store (`hd.session`).
3. `JwtAuthFilter` verifies the token (HS256) and installs a `Caller` (id + role).
4. Controller binds HTTP (`@CurrentCaller`, `@Valid` body) and delegates to the service.
5. Service applies role/ownership/status rules, maps entities to DTOs via repositories.
6. `ApiException` → `{ "code", "message" }` with the mapped status.

## Auth lifecycle

- `POST /auth/login` → `{ token: { value, expiresAt }, user }`. Client stores it; 8 h lifetime.
- No refresh, no logout endpoint, no server session. Logout = client drops the token; expiry forces
  re-login.
- Caller identity is the token subject. Ids in request bodies are never trusted.

## Configuration

| Setting | Where | Notes |
|---|---|---|
| `JWT_SECRET` | env → `app.jwt.secret` | Required real value outside dev; min 32 bytes or the app refuses to start. Dev default lives in `application.yaml`, never commit a real one. |
| `DB_URL` | env → `spring.datasource.url` | Default `jdbc:sqlite:helpdesk.db` next to the working dir. |
| `app.jwt.ttl` | `application.yaml` | `8h`. |
| `/api` proxy target | `hd-web/vite.config.ts` | Dev only: `http://localhost:8080`. |

## Data

SQLite via JPA, `ddl-auto: update` — no migrations tool. Schema: `hd-service/docs/database.md`.
Seeded accounts on first boot (`config/SeedUsers`): `agent@b.co` / `requester@b.co`, password `secret`
— change for any real deployment.

## Run

```bash
cd hd-service && JWT_SECRET=<32+ bytes> ./gradlew bootRun   # API :8080
cd hd-web && bun install && bun run dev                   # UI :3000
```

## Testing

- `hd-service`: `./gradlew test` — MockMvc endpoint tests on H2 (`src/test/resources`), one class per
  endpoint area. CI runs it plus `spotlessCheck` (`.github/workflows/`).
- `hd-web`: no test setup — intended. Verify pages by running the dev server.

## Out of scope by design

Nothing outside `features.md` exists: no pagination, attachments, notifications, audit log, or second
client. Single deployment; scale concerns are deferred until a list is measurably slow.
