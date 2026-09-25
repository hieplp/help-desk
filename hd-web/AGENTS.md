# hd-web

TanStack Start + React 19 + TanStack Router + Tailwind 4. TypeScript, Vite, bun. File-based routing.

## Commands

```bash
bun install              # deps
bun run dev              # dev server on :3000, proxies /api → :8080
bun run build            # production build
bun run generate-routes  # rebuild routeTree.gen.ts after adding a route file
```

## Stack

- `@tanstack/react-router` + `react-start` — file-based routes, SSR shell only
- `@tanstack/react-form` + `zod` — forms and validation
- `zustand` — session store only, persisted to `localStorage` key `hd.session`
- `tailwindcss` v4 via `@tailwindcss/vite` — styling; `demo-*` classes and CSS vars in `src/styles.css`

## Structure

`src/routes/` are thin wrappers: import a feature page, pass to `createFileRoute`. Real pages live in
`src/features/<name>/` with local `components/`, `hooks/`, `schema.ts`. Full rules: `docs/project-structure.md`.

- Routes never call `api/` or hold state.
- `src/api/client.ts` is the only place that fetches — all HTTP goes through `api.*`.
- `src/components/` is feature-agnostic shared UI only.
- `routeTree.gen.ts` is generated; never edit.

## Spec

- `../docs/features.md` — scope; out-of-scope list is binding.
- `../docs/api-rules.md` + `../hd-service/docs/api/*.md` — endpoints, enums, error shape.
- `docs/state.md` — what may persist (session + theme only).
- `docs/security-rules.md` — token handling, XSS, storage.
- `docs/api-client.md` — fetch wrapper conventions.

## Rules

- Token lives only in the session store; sent as `Authorization: Bearer`. Never in URL, params, logs.
- Never send `userId`/`requesterId` — the server derives caller from JWT.
- Branch on HTTP status or error `code`, never on `message` text.
- No state library beyond zustand (session only); no CSS modules; no test setup.
- Branches and commits follow `../docs/rules/git-commit-rules.md` — work on a `<type>/<slug>` branch, never commit
  directly to `main`.
