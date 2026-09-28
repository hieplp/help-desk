# Database — tables

SQLite (`jdbc:sqlite:helpdesk.db`, `DB_URL` overrides). `ddl-auto: update`, no Flyway. Matches
`docs/features.md` and `docs/api-rules.md`. Nothing else.

All three tables exist. `tickets` and `comments` came with their endpoints.

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

Seeded on startup when absent (`config/SeedUsers`): `agent@b.co` / Agent / agent, `requester@b.co` /
Requester / requester — both password `secret`.

## tickets

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
| created_at   | timestamp             | not null, set at insert (`Auditable`)                            |
| updated_at   | timestamp             | not null, set on every write (`Auditable`)                       |

Indexes: `requester_id`, `assignee_id`, `status`.

## comments

Append-only. No edit, no delete.

| column     | type                    | notes                   |
|------------|-------------------------|-------------------------|
| id         | integer PK              | autoincrement           |
| ticket_id  | integer FK → tickets.id | not null                |
| author_id  | integer FK → users.id   | not null                |
| body       | varchar(2000)           | not null                |
| created_at | timestamp               | not null, set at insert |

Index: `ticket_id`.

## Notes

- `updated_at` on tickets drives list ordering (newest first).
- No `updated_at` on comments — they never change.
- FK deletes: restrict. Tickets and comments are never deleted.
