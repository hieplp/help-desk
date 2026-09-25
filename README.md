# help-desk

Internal help desk: requesters file tickets, agents triage and resolve them.
Two apps — `hd-service/` Spring Boot backend and `hd-web/` TanStack Start frontend.

## Layout

| Path | What |
|---|---|
| `hd-service/` | Spring Boot REST API — Java 25, Gradle, SQLite, JWT auth |
| `hd-web/` | TanStack Start + React 19 + Tailwind 4 — TypeScript, Vite, bun |
| `docs/` | Product and API spec — **binding**; code follows it |
| `docs/rules/` | Validation and git commit rules |

## Spec

Read before writing code:

- [`docs/requirements.md`](docs/requirements.md) — business requirements, actors, user stories
- [`docs/features.md`](docs/features.md) — scope; the out-of-scope list is binding
- [`docs/api-rules.md`](docs/api-rules.md) — endpoints, enums, error shape
- [`docs/rules/validation-rules.md`](docs/rules/validation-rules.md) — input rules
- [`docs/rules/git-commit-rules.md`](docs/rules/git-commit-rules.md) — Conventional Commits

Behavior must differ from spec? Change the doc first.

## Run the backend

```bash
cd hd-service
./gradlew bootRun   # API on :8080
./gradlew test      # tests
./gradlew build     # compile + test
```

Users are seeded — no self-registration. Login returns a JWT; send it as
`Authorization: Bearer <jwt>`. Details: `hd-service/AGENTS.md`.

## Run the frontend

```bash
cd hd-web
bun install
bun run dev    # dev server on :3000, proxies /api → :8080
bun run build  # production build
```

Start the backend first — the frontend talks to it through the `/api` proxy.
Details: `hd-web/AGENTS.md`.

## Roles

- **requester** — creates tickets, sees and comments on their own, can close them
- **agent** — sees all tickets, moves status, assigns, comments anywhere

## Contributing

- Issues: use the Feature/Bug templates — every feature cites its spec section.
- PRs: template enforces spec match + verified steps.
- Branches: `<type>/<slug>`, never commit to `main` — see `docs/rules/git-commit-rules.md`.
- Commits: Conventional Commits, one concern each.
- Git hooks: run `git config core.hooksPath .githooks` to enforce formatting (`spotlessCheck`) and unit tests before commit/push.
