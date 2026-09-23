# Prompt: new API endpoint

Copy this file, fill in the `<...>` slots, delete what does not apply, then hand it to the agent.

---

Implement `<METHOD> <PATH>` in `hd-service`.

## Endpoint

- Method + path: `<METHOD> <PATH>`
- Roles: `<requester | agent | any authenticated | public>`
- Row scope: `<which rows the caller may see/act on; e.g. requester: own only, else 404>`
- Request body: `<fields, or none>`
- Success: `<status>` → `<response shape>`
- Errors: `<status → when, e.g. 400 bad enum, 403 wrong role, 404 missing>`

## Rules to follow

- `docs/api-rules.md` — error body `{ "code": "...", "message": "..." }`, status codes, enums.
- `docs/security-rules.md` — caller comes from the JWT subject, never from body/query. Service re-checks role, not just
  the controller. `404` over `403` when `403` would confirm existence.
- `../docs/rules/validation-rules.md` — trim strings, reject unknown fields, enum values exact lowercase, first failure
  wins.
- `docs/project-structure.md` — `controller` → `service` → `repository` → `model`; `dto` crosses the boundary, never
  return an entity; one class per resource per layer.
- `docs/database.md` — schema. If a new column/table is needed, update this doc first.
- Out of scope: `../docs/features.md` — nothing beyond it (no pagination, versioning, extra filters).

## Steps

1. If the endpoint is not yet in `docs/api/<resource>.md`, add it there first in the same format as `tickets.md`.
2. DTOs in `dto/` (request + response), bean validation where it fits.
3. `<Resource>Controller` — parse input, call service, return dto. No repository access, no header reading.
4. `<Resource>Service` (interface) + `<Resource>ServiceImpl` — role and row-scope checks, business rules, entity ↔ dto
   mapping. Controller depends on the interface.
5. `<Resource>Repository` — Spring Data interface, only if a new query is needed.
6. Entity changes in `model/` only if the schema requires it (update `docs/database.md` too).

## Verify

- `./gradlew build` passes.
- Smoke test with `bootRun` + `curl`: success path, one `400`, one `403`/`404` as applicable.
- Commit: `feat: add <METHOD> <PATH>` per `../docs/rules/git-commit-rules.md`.
