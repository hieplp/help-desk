# Help desk

Two apps: web frontend (not yet created) and `hd-service/` backend. Two roles: `requester` and `agent`.

## Layout

- `hd-service/` — Spring Boot backend. See `hd-service/AGENTS.md`.
- `docs/` — product and API spec.
- `docs/rules/` — validation and git commit rules.

## Read first

- `docs/features.md` — scope. Out-of-scope list is binding.
- `docs/api-rules.md` — endpoints, enums, error shape.
- `docs/rules/validation-rules.md` — input rules.
- `docs/rules/git-commit-rules.md` — Conventional Commits: `feat`/`fix`/`docs`/`chore`/`refactor`/`test`.

## Rules

- Spec lives in `docs/`. Code follows it; change the doc first if behavior must differ.
- No pagination, versioning, or features beyond `docs/features.md`.
