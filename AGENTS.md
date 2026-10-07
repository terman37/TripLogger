# AGENTS.md — TripToExcel (Android)

Guidelines for AI agents and contributors working in this repository. Applies in
addition to the global agent rules (`~/.pi/agent/AGENTS.md`).

## Project overview

Android app (Kotlin) that logs car trips and exports expense-report spreadsheets.
See [README.md](README.md) (short front page, links to the hosted docs) and
[docs/DETAILS.md](docs/DETAILS.md) (technical documentation) for the current
behaviour. The user guide is [docs/user-guide.md](docs/user-guide.md) (French,
French-first decision) — its English translation is [docs/en/user-guide.md](docs/en/user-guide.md).
The owner is **new to Android development**: code must be
understandable, generously commented, and behavior must be testable.

## Workflow rules — do not bypass

1. The owner reviews and commits **each step separately**. Split work into small,
   reviewable steps and stop after each step for review.
2. For multi-step work, keep a short checklist in the task/PR description. The
   working plan for the Play release is `release_guide.md`: it is gitignored
   (local-only, not part of the repository) and replaced the original plan.md.
3. Do not implement a step the owner has not approved.
4. If a new issue changes scope or exposes an unresolved requirement, stop and ask.

## Current status

- Working app: trip logging, Report/Excel export, Settings tab (device setup,
  reconnect grace period, About), dark Compose UI
  (validated on a real device). See docs/user-guide.md (French) or
  docs/en/user-guide.md (English) for the user guide, and docs/DETAILS.md for the
  technical documentation. The app ships French-first with English in
  `res/values-en/`; the UI strings live in `res/values/strings.xml` (French).
- Package: `com.terman37.triplogger`. minSdk 34, targetSdk 37.
- Version catalog: `gradle/libs.versions.toml` (single source of dependency
  versions). Add new libraries there, never hardcode versions in build files.

## When asked to create a new AAB (do all of this, in order)

1. **Bump `versionCode`** in `app/build.gradle.kts` — Play requires it to be higher
   than every bundle already uploaded, including to an internal/closed testing
   track. Leave `versionName` alone unless the user-visible version really changes
   (1.0 → 1.1); the two numbers are different things.
2. **Update the release notes in both languages**:
   `docs/release-notes.md` (French) and `docs/en/release-notes.md` (English).
   Add an entry for the new version — same tone as the existing ones, user-visible
   changes first, no commit hashes and no developer jargon. These files are the
   single source for the Play Console's "What's new" field in each language, so
   they must be written before the bundle is built, not after.
   **The text must be ≤ 500 characters per language** (Play's limit — the Console
   rejects a longer note with "La note de version … est trop longue"). Put the
   pasteable text in a fenced block and state its count in the entry, then verify
   the count with:
   `python3 -c "import re,pathlib;t=pathlib.Path('docs/release-notes.md').read_text();print(len(re.search(r'\`\`\`\n(.*?)\n\`\`\`',t,re.S).group(1)))"`
   The same text is duplicated in `release_guide.md` Appendix A.2/A.3 — keep the
   two in sync (Appendix A is the English/French listing copy, the docs pages are
   what the user reads on the site).
3. **Build and verify**: `./gradlew :app:bundleRelease`, then check the signature
   (`keytool -printcert -jarfile`), the `application-label` and the version
   (`aapt2 dump badging` on the matching APK), and that `SampleDataSeeder` is
   absent from `base/dex`. Report the AAB path, its size and the
   `app/build/outputs/mapping/release/mapping.txt` file that must be kept for that
   build's crash reports.
4. **Tell the user what to paste** into the Console: the new "What's new" text per
   language, and remind them that the upload needs a rollout on the chosen track.

Keep the plan (`release_guide.md`, gitignored) in sync with what was done.

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
- **Dependencies:** declare in `gradle/libs.versions.toml`; update the docs if
  architecture or setup changes.
- **Files:** README.md (repo front page) and docs/ (user guide, technical,
  interface, builds, privacy policy, site config) are source-of-truth docs. The
  user-facing pages exist in both languages: French at the root, English under
  `docs/en/`; technical docs are English only. The docs are published by GitHub
  Pages; keep them in sync with code behavior.

## Validation

- `./gradlew :app:testDebugUnitTest` — JVM unit tests
- `./gradlew :app:assembleDebug` — build APK (see docs/BUILD.md)
- Lint/compile errors must be fixed before marking any change done.

## Pitfalls to respect

- Bluetooth + location on Android 14+ (minSdk 34): runtime permissions
  (`BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS`) and
  foreground-service types must be declared correctly; see docs/DETAILS.md
  ("Permissions") and `release_guide.md` (Play release plan).
- Do not log locations/addresses or other personal data unnecessarily.
- No server/account: data is local. Keep it that way unless asked otherwise.
