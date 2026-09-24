# help-desk

Internal help desk: requesters file tickets, agents triage and resolve them.
Two apps — `hd-service/` Spring Boot backend and a web frontend (not yet created).

## Layout

| Path | What |
|---|---|
| `hd-service/` | Spring Boot REST API — Java 25, Gradle, SQLite, JWT auth |
| `docs/` | Product and API spec — **binding**; code follows it |
| `docs/rules/` | Validation and git commit rules |

## Spec

Read before writing code:

- [`docs/features.md`](docs/features.md) — scope; the out-of-scope list is binding
- [`docs/api-rules.md`](docs/api-rules.md) — endpoints, enums, error shape
- [`docs/rules/validation-rules.md`](docs/rules/validation-rules.md) — input rules
- [`docs/rules/git-commit-rules.md`](docs/rules/git-commit-rules.md) — Conventional Commits

Behavior must differ from spec? Change the doc first.

## Run the backend

```bash
cd hd-service
./gradlew bootRun   # run
./gradlew test      # tests
./gradlew build     # compile + test
```

Users are seeded — no self-registration. Login returns a JWT; send it as
`Authorization: Bearer <jwt>`. Details: `hd-service/AGENTS.md`.

## Roles

- **requester** — creates tickets, sees and comments on their own, can close them
- **agent** — sees all tickets, moves status, assigns, comments anywhere

## Contributing

- Issues: use the Feature/Bug templates — every feature cites its spec section.
- PRs: template enforces spec match + verified steps.
- Commits: Conventional Commits, one concern each — see `docs/rules/git-commit-rules.md`.
