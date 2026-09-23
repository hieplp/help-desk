# Database — tables

SQLite (`jdbc:sqlite:helpdesk.db`, `DB_URL` overrides). `ddl-auto: update`, no Flyway. Matches
`docs/features.md` and `docs/api-rules.md`. Nothing else.

Only `users` exists in code today. `tickets` and `comments` below are the planned schema — create them with their
endpoints, not before.

## users

Seeded accounts. No self-registration.

| column        | type         | notes                            |
|---------------|--------------|----------------------------------|
| id            | integer PK   | autoincrement                    |
| name          | varchar(120) | not null                         |
| email         | varchar(255) | not null, unique, lowercase      |
| password_hash | varchar(100) | not null, bcrypt                 |
| role          | varchar(20)  | not null: `REQUESTER` \| `AGENT` |

`role` is stored as the enum `name()` (uppercase) via `@Enumerated(STRING)`; JSON serializes it lowercase.

Seeded on startup when absent (`config/SeedUsers`): `a@b.co` / Ada / agent, `b@b.co` / Bea / requester — both password
`secret`.

## tickets (planned)

| column       | type                  | notes                                                              |
|--------------|-----------------------|--------------------------------------------------------------------|
| id           | integer PK            | autoincrement                                                      |
| title        | varchar(120)          | not null                                                           |
| description  | varchar(4000)         | not null                                                           |
| category     | varchar(20)           | not null: `hardware` `software` `access` `other`                   |
| priority     | varchar(20)           | not null: `low` `medium` `high`                                    |
| status       | varchar(20)           | not null, default `open`: `open` `in_progress` `resolved` `closed` |
| requester_id | integer FK → users.id | not null                                                           |
| assignee_id  | integer FK → users.id | null = unassigned                                                  |
| created_at   | timestamp             | not null, default now()                                            |
| updated_at   | timestamp             | not null, default now()                                            |

Indexes: `requester_id`, `assignee_id`, `status`.

## comments (planned)

Append-only. No edit, no delete.

| column     | type                    | notes                   |
|------------|-------------------------|-------------------------|
| id         | integer PK              | autoincrement           |
| ticket_id  | integer FK → tickets.id | not null                |
| author_id  | integer FK → users.id   | not null                |
| body       | varchar(2000)           | not null                |
| created_at | timestamp               | not null, default now() |

Index: `ticket_id`.

## Notes

- `updated_at` on tickets drives list ordering (newest first).
- No `updated_at` on comments — they never change.
- FK deletes: restrict. Tickets and comments are never deleted.
