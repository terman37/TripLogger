# AGENTS.md — Trip Logger (Android)

Guidelines for AI agents and contributors working in this repository. Applies in
addition to the global agent rules (`~/.pi/agent/AGENTS.md`).

## Project overview

Android app (Kotlin) that logs car trips and exports expense-report spreadsheets.
See [README.md](README.md) (architecture + how it works) and [todo.md](todo.md)
(user requirements). The owner is **new to Android development**: code must be
understandable, generously commented, and behavior must be testable.

## Workflow rules (from todo.md — do not bypass)

1. The owner reviews and commits **each step separately**. Split work into small,
   reviewable steps and stop after each step for review.
2. For multi-step work, keep a short checklist in the task/PR description (there
   is no plan file: the original plan.md was completed and removed).
3. Do not implement a step the owner has not approved.
4. If a new issue changes scope or exposes an unresolved requirement, stop and ask.

## Current status

- Working app: trip logging, Report/Excel export, Devices setup, dark Compose UI
  (validated on a real device). See README.md for the user guide and
  DETAILS.md for the technical documentation.
- Package: `com.terman37.triplogger`. minSdk 34, targetSdk 37.
- Version catalog: `gradle/libs.versions.toml` (single source of dependency
  versions). Add new libraries there, never hardcode versions in build files.

## Conventions

- **Language:** Kotlin. Follow official Kotlin code style (`kotlin.code.style=official`).
- **Comments:** explain *why*, not what. This owner is a beginner: document
  non-obvious Android concepts (lifecycle, services, permissions, intents) when
  touched.
- **Architecture:** keep Android-specific code thin and behind interfaces so core
  logic (trip state machine, distance, Excel, queries) is unit-testable on the JVM.
  Suggested split in README "Architecture overview".
- **Names/types:** strict useful types. No `Any`, `unknown`, or unstructured maps
  where a precise type is practical.
- **Tests:** unit tests live in `app/src/test`, instrumented in `app/src/androidTest`.
  Add tests for changed behavior. Run the narrowest relevant checks first.
- **Dependencies:** declare in `gradle/libs.versions.toml`; update README if
  architecture or setup changes.
- **Files:** README.md (user guide), DETAILS.md (technical), UI.md (interface),
  BUILD.md (builds), todo.md (backlog) are source-of-truth docs. Keep in sync
  with code behavior.

## Validation

- `./gradlew :app:testDebugUnitTest` — JVM unit tests
- `./gradlew :app:assembleDebug` — build APK (see BUILD.md)
- Lint/compile errors must be fixed before marking any change done.

## Pitfalls to respect

- Bluetooth + location on Android 14+ (minSdk 34): runtime permissions
  (`BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS`) and
  foreground-service types must be declared correctly; see todo.md open questions.
- Do not log locations/addresses or other personal data unnecessarily.
- No server/account: data is local. Keep it that way unless asked otherwise.
