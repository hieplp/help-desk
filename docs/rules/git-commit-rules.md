# Help desk — branch & git commit rules

Conventional Commits. Matches existing history (`feat:`, `docs:`, `chore:`). Nothing else.

## Format

```
<type>: <subject>
```

- `type`: `feat` | `fix` | `docs` | `chore` | `refactor` | `test`
- Subject: imperative, lowercase, no trailing period, max 72 chars.
- One concern per commit. Split unrelated changes.
- No scope, no body, no footer — add them when a commit actually needs explanation.

## Branches

```
<type>/<slug>
```

- `type`: same set as commit types (`feat`, `fix`, `docs`, `chore`, `refactor`, `test`).
- `slug`: short kebab-case description, e.g. `feat/ticket-comments`.
- One branch per concern; branch names match the work, not the person.
- Delete the branch after merge.
- No `wip`, `temp`, `test123`, or personal-name branches.

## Types

- `feat`: new behavior or endpoint.
- `fix`: bug fix.
- `docs`: documentation only.
- `chore`: tooling, config, dependencies, IDE files.
- `refactor`: code change, no behavior change.
- `test`: tests only.

## Examples

```
feat: add PATCH /tickets/:id endpoint
fix: reject empty comment body
docs: add validation rules
chore: add IDE and AI tool configs
```

## Rules

- Commit only working code — project compiles, no half-edits.
- Never commit secrets, `.env`, tokens, or local credentials.
- Generated/build output stays out (`bin/`, `.gradle/`, `build/`).
- No `WIP`, `temp`, `asdf` messages. Stash instead.
