# Tickets

Client calls go through `api.*` in `../../src/api/client.ts`. Contract: `../../hd-service/docs/api/tickets.md`.

- Create: `api.post('/tickets', { title, description, category, priority })` — implemented at `/tickets/new`
  (`features/tickets/new`).
- List with status filter: `api.get('/tickets?status=...')` — not implemented yet.
- Detail: `api.get('/tickets/{id}')` — not implemented yet.
- Patch status/assignee: `api.patch('/tickets/{id}', { status?, assigneeId? })` — not implemented yet.
- Add comment: `api.post('/tickets/{id}/comments', { body })` — not implemented yet.

All calls must respect the row-scope rules in the service contract (requester sees own only, etc.). UI must not send caller ids.