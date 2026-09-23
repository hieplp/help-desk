# Database — tables

PostgreSQL. Matches `docs/features.md` and `docs/api-rules.md`. Nothing else.

Three tables: `users`, `tickets`, `comments`. Enums are varchar columns, not lookup tables.

## users

Seeded accounts. No self-registration.

| column         | type         | notes                              |
|----------------|--------------|------------------------------------|
| id             | bigint PK    | generated                          |
| name           | varchar(120) | not null                           |
| email          | varchar(255) | not null, unique, lowercase        |
| password_hash  | varchar(100) | not null, bcrypt                   |
| role           | varchar(20)  | not null: `requester` \| `agent`   |

## tickets

| column       | type          | notes                                        |
|--------------|---------------|----------------------------------------------|
| id           | bigint PK     | generated                                    |
| title        | varchar(120)  | not null                                     |
| description  | varchar(4000) | not null                                     |
| category     | varchar(20)   | not null: `hardware` `software` `access` `other` |
| priority     | varchar(20)   | not null: `low` `medium` `high`              |
| status       | varchar(20)   | not null, default `open`: `open` `in_progress` `resolved` `closed` |
| requester_id | bigint FK → users.id | not null                              |
| assignee_id  | bigint FK → users.id | null = unassigned                       |
| created_at   | timestamptz   | not null, default now()                      |
| updated_at   | timestamptz   | not null, default now()                      |

Indexes: `requester_id`, `assignee_id`, `status`.

## comments

Append-only. No edit, no delete.

| column      | type          | notes                    |
|-------------|---------------|--------------------------|
| id          | bigint PK     | generated                |
| ticket_id   | bigint FK → tickets.id | not null         |
| author_id   | bigint FK → users.id   | not null         |
| body        | varchar(2000) | not null                 |
| created_at  | timestamptz   | not null, default now()  |

Index: `ticket_id`.

## Notes

- `updated_at` on tickets drives list ordering (newest first).
- No `updated_at` on comments — they never change.
- FK deletes: restrict. Tickets and comments are never deleted.
