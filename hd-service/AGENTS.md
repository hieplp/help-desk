# hd-service

Spring Boot 4.1.1 REST API. Java 25, Gradle Kotlin DSL, SQLite, Lombok. JSON only, no views.

## Commands

```bash
./gradlew build    # compile + test
./gradlew test     # tests
./gradlew bootRun  # run
```

## Stack

- `spring-boot-starter-webmvc` — REST controllers
- `spring-boot-starter-data-jpa` + SQLite — persistence
- `spring-boot-starter-security` — JWT auth
- `spring-boot-starter-validation` — bean validation
- Lombok — `compileOnly` + `annotationProcessor`, both already wired

## Structure

Layer packages under `dev.hieplp.helpdesk`: `controller` → `service` → `repository` → `model`. `dto` crosses the HTTP
boundary; `config` holds security/JWT; `exception` holds the error handler. Full rules: `docs/project-structure.md`.

- Controllers never touch repositories; repositories never call services.
- Never return an entity from a controller — map to `dto`.
- One class per resource per layer. No generic base classes.
- Comments live on the ticket controller/service, not a second stack.

## Spec

- `docs/api-rules.md` — endpoints, enums, error body `{ "code": "...", "message": "..." }`.
- `docs/security-rules.md` — JWT, roles, access rules.
- `docs/database.md` — schema.
- `../docs/rules/validation-rules.md` — input limits.

## Rules

- Auth is JWT (`Authorization: Bearer`), parsed in `config`. Controllers read the authenticated caller, not the header.
- Errors: `400` bad input, `401` bad/missing token, `403` wrong role, `404` missing — always
  `{ "code": "...", "message": "..." }`.
- `static/` and `templates/` stay empty.
- Branches and commits follow `../docs/rules/git-commit-rules.md` — work on a `<type>/<slug>` branch, never commit
  directly to `main`.
