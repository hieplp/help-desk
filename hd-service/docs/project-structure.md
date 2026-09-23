# Project structure

One Gradle project. Simple MVC. Layer packages, not feature packages.

There is no view. The controller returns JSON.

## Root

```
hd-service/
  build.gradle.kts
  settings.gradle.kts
  gradlew, gradlew.bat
  gradle/wrapper/
  docs/
  src/main/java/
  src/main/resources/application.yaml
  src/test/java/
```

`build/`, `.gradle/`, `bin/`, `.idea/`, and the Eclipse files are generated. Ignore them.

`templates/` and `static/` stay empty — a REST API has no view layer. `db/migration/` is not used: `ddl-auto: update`,
no Flyway.

## Code

```
src/main/java/dev/hieplp/helpdesk/
  HdServiceApplication.java
  config/         SecurityConfig, SeedUsers
  controller/     one REST controller per resource
  service/        rules: who may do what, status changes; impl/ for implementations
  repository/     Spring Data interfaces, one per entity
  model/
    entity/       JPA entities
    dto/          request and response types
    enums/        enums stored as lowercase names
  exception/      ApiException and the handler
  security/       JwtService, JwtAuthFilter
  common/         shared helpers (MaxBytes validator)
```

- A request enters `controller`, calls `service`, which uses `repository` and `model/entity`.
- Controllers do not touch repositories.
- Repositories do not call services.
- `model/dto` is what crosses the HTTP boundary. `model/entity` is what is stored. Do not return an entity from a
  controller.
- JWT parsing stays in `security`. Controllers read the authenticated caller; they do not read the header.
- One class per resource in each layer (`TicketController`, `TicketService`, `TicketRepository`, `Ticket`). No generic
  base classes.
- Comments are a `model` and methods on the ticket controller and service, not a second stack, until that file is doing
  two jobs.

## Tests

Same packages under `src/test/java/dev/hieplp/helpdesk/`.

## Not used

Feature packages, `api` / `domain` / `persistence` slices, and extra Gradle modules.
