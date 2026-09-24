# Prompt: new page

Copy this file, fill in the `<...>` slots, delete what does not apply, then hand it to the agent.

---

Implement `<ROUTE PATH>` page in `hd-web`.

## Page

- Route path: `<ROUTE PATH>`
- Feature: `<feature name, e.g. tickets>`
- Roles that may see it: `<requester | agent | any authenticated | public>`
- API calls it makes: `<list: e.g. GET /tickets, POST /tickets/{id}/comments>`

## Rules to follow

- `../project-structure.md` — routes/ thin (createFileRoute + import), features/<feature>/ (index.tsx + components/ + hooks/ + schema.ts), api/ for all calls, components/ shared only.
- `../api-client.md` — every HTTP call uses `api.*` from `#/api/client`; never raw fetch; caller identity from token.
- `../security-rules.md` — token only via header; never in URL/params; logout = setSession(null); no dangerouslySetInnerHTML on user data.
- `../state.md` — only session and theme in localStorage; server is truth for domain data.
- `../../docs/rules/validation-rules.md` — client validation (zod schema in feature) is convenience only, never a substitute for server.
- `../../docs/rules/git-commit-rules.md` — conventional commits.
- `../../hd-service/docs/api/*.md` — contract for the calls; update or reference the service spec first.
- `../api/<resource>.md` — add the thin client description here if the call is new.

## Steps

1. Add feature dir under `src/features/<feature>/`: `index.tsx` (page), `components/` (local only), `hooks/` and `schema.ts` as needed (zod).
2. Add thin route file `src/routes/<name>.tsx` that does `createFileRoute(...)` importing the page from features.
3. Run `bun run generate-routes` to update `routeTree.gen.ts`.
4. Wire API calls exclusively through `src/api/client.ts` (or extend it if a new verb is required).
5. Add or update the thin client doc in `docs/api/<resource>.md`.

## Verify

- `bun run dev` smoke test: load the route, exercise happy path + one error case per role.
- Protected routes: unauthenticated redirect or hide; authenticated but wrong role still reaches server `403`/`404`.
- Commit: `feat: add <ROUTE PATH> page` per `../../docs/rules/git-commit-rules.md`.