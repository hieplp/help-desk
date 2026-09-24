# Tickets

Not implemented yet — this is the spec. Client calls will go through `api.*` in `../../src/api/client.ts`. Contract: `../../hd-service/docs/api/tickets.md`.

- List with status filter: `api.get('/tickets?status=...')`
- Detail: `api.get('/tickets/{id}')`
- Create: `api.post('/tickets', { title, description, category, priority })`
- Patch status/assignee: `api.patch('/tickets/{id}', { status?, assigneeId? })`
- Add comment: `api.post('/tickets/{id}/comments', { body })`

All calls must respect the row-scope rules in the service contract (requester sees own only, etc.). UI must not send caller ids.