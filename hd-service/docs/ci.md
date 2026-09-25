# hd-service — CI

Two GitHub Actions workflows under `.github/workflows/`. Both run on `push` and `pull_request` to `main`
when `hd-service/**` (or the workflow file itself) changes. Both use `actions/setup-java` with Temurin 25
and the Gradle wrapper (`./gradlew`, working directory `hd-service`).

## Code style — `hd-service-codestyle.yml`

Enforces [Google Java Style](https://google.github.io/styleguide/javaguide.html) via
[Spotless](https://github.com/diffplug/spotless) (`com.diffplug.spotless` 7.2.1) with
`googleJavaFormat("1.28.0")`, configured in `build.gradle.kts`.

What google-java-format enforces (not configurable — it's the Google style, applied mechanically):

- 2-space indentation, 4-space continuation indent.
- 100-column line limit; long lines are re-wrapped.
- Import order: all `static` imports first, then non-static, ASCII sort; unused imports removed.
- Braces on the same line (K&R); braces required even for single-statement bodies.
- One blank line between members; no wildcard imports.

The version is pinned at 1.28.0 because the Spotless default crashes on JDK 25 javac internals
(`NoSuchMethodError` on `Log$DeferredDiagnosticHandler`). palantir-java-format has the same crash —
not an option here.

Two local rules ride on top via a custom Spotless step in `build.gradle.kts` (`localStyle`):

- A parameter list wrapped across lines puts `) {` on its own line at declaration indent.
- A Javadoc `@tag` continuation aligns under the tag argument (`*         text`, not `*     text`).

**Workflow** — `actions/checkout@v4` → `actions/setup-java@v4` (Temurin 25) → `./gradlew spotlessCheck`.
Fails the check on any violation.

**Local**

```bash
./gradlew spotlessCheck  # verify — same command CI runs
./gradlew spotlessApply  # auto-fix all violations
```

Never edit around the formatter — run `spotlessApply` and commit the result.

## Unit tests — `hd-service-test.yml`

**Workflow** — `actions/checkout@v4` → `actions/setup-java@v4` (Temurin 25) → `./gradlew test`.
Fails the build on any test failure. Gradle's existing `useJUnitPlatform()` config is unchanged;
tests run against H2 (`testRuntimeOnly`), not the SQLite file.
