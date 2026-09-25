# hd-service

Spring Boot 4.1.1 REST API for the help desk app. Java 25, Gradle Kotlin DSL, SQLite, Lombok. JSON only, no views.

## Commands

```bash
./gradlew build          # compile + test
./gradlew test           # unit tests
./gradlew bootRun        # run locally
./gradlew spotlessCheck  # verify code style (what CI runs)
./gradlew spotlessApply  # auto-fix code style
```

## Code style

[Google Java Style](https://google.github.io/styleguide/javaguide.html), enforced by
[Spotless](https://github.com/diffplug/spotless) + google-java-format 1.28.0 plus two local rules
(wrapped-parameter `) {` line, Javadoc tag-continuation alignment) in `build.gradle.kts`.
Run `./gradlew spotlessApply` before committing — CI fails on violations.

## CI

Two GitHub Actions workflows (`.github/workflows/`), triggered on push/PR to `main` when `hd-service/**` changes:

- `hd-service-codestyle.yml` — `./gradlew spotlessCheck`
- `hd-service-test.yml` — `./gradlew test`

Details: `docs/ci.md`.

## Docs

- `docs/api-rules.md` — endpoints, enums, error shape
- `docs/security-rules.md` — JWT, roles, access rules
- `docs/database.md` — schema
- `docs/project-structure.md` — layer/package rules
- `docs/ci.md` — CI workflows and code style
- `../docs/rules/validation-rules.md` — input limits
