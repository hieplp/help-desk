# Client state — persistence

Zustand + localStorage only. Matches `../../docs/features.md`. Server is source of truth for tickets, users, and comments. Nothing else persists.

## Session

Persisted under `localStorage` key `hd.session` via zustand `persist` middleware.

| field     | type                           | notes                                      |
|-----------|--------------------------------|--------------------------------------------|
| token     | string                         | JWT; never log or put in URL               |
| user.id   | number                         | integer from server                        |
| user.name | string                         |                                                |
| user.email | string                         |                                                |
| user.role | 'requester' \| 'agent'         | lower case                                 |

`getSession()` and `setSession()` are the only accessors. Used by `api/client.ts` and route components.

Login sets it from `POST /auth/login` response. Logout calls `setSession(null)`.

## Theme

`localStorage` key `theme`.

| value | meaning                              |
|-------|--------------------------------------|
| light | force light                          |
| dark  | force dark                           |
| auto  | follow `prefers-color-scheme` (default) |

Resolved before first paint by the inline script in `src/routes/__root.tsx`.

`ThemeToggle` reads, writes, and applies it. Applies class `light`/`dark` on `<html>` and `data-theme` attr when not auto.

## Not used

- No other keys in localStorage or sessionStorage.
- No IndexedDB, no cookies for app data.
- No in-memory-only stores beyond React state inside a feature.
- Tickets, comments, user lists: fetched on demand; never cached in client storage.