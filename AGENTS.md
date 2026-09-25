# Help desk

Two apps: `hd-web/` frontend and `hd-service/` backend. Two roles: `requester` and `agent`.

## Layout

- `hd-service/` — Spring Boot backend. See `hd-service/AGENTS.md`.
- `hd-web/` — TanStack Start frontend. See `hd-web/AGENTS.md`.
- `docs/` — product and API spec.
- `docs/rules/` — validation, branch, and git commit rules.

## Read first

- `docs/requirements.md` — business requirements, actors, user stories.
- `docs/features.md` — scope. Out-of-scope list is binding.
- `docs/api-rules.md` — endpoints, enums, error shape.
- `docs/rules/validation-rules.md` — input rules.
- `docs/rules/git-commit-rules.md` — branch naming + Conventional Commits: `feat`/`fix`/`docs`/`chore`/`refactor`/`test`.

## Rules

- Spec lives in `docs/`. Code follows it; change the doc first if behavior must differ.
- No pagination, versioning, or features beyond `docs/features.md`.
- Before committing: work on a `<type>/<slug>` branch (never commit directly to `main`), and follow
  `docs/rules/git-commit-rules.md` for both branch name and commit message.
- Issues use the matching `.github/ISSUE_TEMPLATE/` (`feature`/`bug`/`docs`/`chore`/`refactor`/`test`); PRs follow
  `.github/pull_request_template.md` (What / Spec / Verified / Checklist).
