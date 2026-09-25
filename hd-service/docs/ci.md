# hd-service — CI

Two GitHub Actions workflows under `.github/workflows/`. Both run on `push` and `pull_request` to `main`
when `hd-service/**` (or the workflow file itself) changes. Both use `actions/setup-java` with Temurin 25
and the Gradle wrapper (`./gradlew`, working directory `hd-service`).

## Code style — `hd-service-codestyle.yml`

- Tool: Spotless (`com.diffplug.spotless`) with `googleJavaFormat()`, configured in `build.gradle.kts`.
- Workflow runs `./gradlew spotlessCheck`.
- Fix violations locally with `./gradlew spotlessApply` — never edit around the formatter.

## Unit tests — `hd-service-test.yml`

- Workflow runs `./gradlew test`.
- Fails the build on any test failure; Gradle's JUnit Platform config is unchanged.
