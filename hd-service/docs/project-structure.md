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

`static/` and `templates/` stay empty.

## Code

```
src/main/java/dev/hieplp/helpdesk/
  HdServiceApplication.java
  controller/     one REST controller per resource
  service/        rules: who may do what, status changes
  repository/     Spring Data interfaces, one per model
  model/          JPA entities
  dto/            request and response types
  config/         security, JWT, CORS
  exception/      error body and the handler
```

- A request enters `controller`, calls `service`, which uses `repository` and `model`.
- Controllers do not touch repositories.
- Repositories do not call services.
- `dto` is what crosses the HTTP boundary. `model` is what is stored. Do not return an entity from a controller.
- JWT parsing stays in `config`. Controllers read the authenticated caller; they do not read the header.
- One class per resource in each layer (`TicketController`, `TicketService`, `TicketRepository`, `Ticket`). No generic base classes.
- Comments are a `model` and methods on the ticket controller and service, not a second stack, until that file is doing two jobs.

## Tests

Same packages under `src/test/java/dev/hieplp/helpdesk/`.

## Not used

Feature packages, `api` / `domain` / `persistence` slices, and extra Gradle modules.
