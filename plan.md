# Trip Logger — implementation plan

Step-by-step plan to build the app. Source of truth: [todo.md](todo.md)
(requirements), [UI.md](UI.md) (visual spec), [README.md](README.md)
(architecture).

**Workflow:** one step at a time. After each step: owner reviews and commits,
plan.md updated (this file), then next step starts. Never start a step that was
not approved. Every step must leave the project compiling:
`./gradlew :app:assembleDebug` and unit tests `./gradlew :app:testDebugUnitTest`.

## Phase 0 — foundations (done)

- [x] Gradle project skeleton (Android Studio template, minSdk 34 / targetSdk 37)
- [x] README.md, AGENTS.md, todo.md requirements, UI.md visual spec
- [x] Architecture + behavior decisions (see README/todo "Done" sections)

## Phase 1 — skeleton, data, core logic (no UI yet)

### [x] Step 1: Compose + dark theme + bottom navigation shell
- Goal: empty app with 3 tabs (Home / Devices / Report) navigating between
  placeholder screens, Material 3 dark theme, English.
- Key files: `app/build.gradle.kts` (Compose deps), `gradle/libs.versions.toml`
  (versions), `MainActivity.kt`, `ui/theme/*`, `ui/TripLoggerApp.kt`,
  `ui/screens/*`.
- Notes: AGP 9.4 uses built-in Kotlin; exact way to enable Compose + the Kotlin
  Compose compiler plugin must be researched in this step (AGP 9 changes this).
  Add `androidx.activity:activity-compose`, Compose BOM, material3,
  `navigation-compose`, `lifecycle-viewmodel-compose` to the version catalog.
- Validation: `./gradlew :app:assembleDebug`; unit tests still pass.
- Review: open app in emulator → dark theme, 3 tabs switch.

### [x] Step 2: Trip database (Room)
- Goal: typed storage for trips.
- Key files: `data/Trip.kt` (entity: start/end epoch ms, start/end lat+lng,
  start/end street, start/end city, distanceKm, origin auto/manual),
  `data/TripDao.kt` (insert, trips in date range ascending, trips since date,
  delete by id), `data/TripDatabase.kt`.
- Notes: add Room runtime + KSP plugin to catalog. In-memory DAO tests need an
  Android context → covered by instrumented test (`androidTest`, run on
  emulator/device); JVM tests start at Step 3.
- Validation: `:app:assembleDebug`; instrumented test file compiles.

### [ ] Step 3: Core pure logic — distance + filters + CSV-independent models
- Goal: JVM-testable calculation units.
- Key files: `core/DistanceCalculator.kt` (Haversine), `core/LocationFilter.kt`
  (keep fix if ≥ 10 m displacement, accuracy ≤ 50 m, implied speed ≤ 160 km/h),
  `core/models.kt` (GpsSample, TripDraft).
- Tests: `core/DistanceCalculatorTest.kt`, `LocationFilterTest.kt`
  (fixed known inputs, e.g. 1° latitude ≈ 111.19 km).
- Validation: `./gradlew :app:testDebugUnitTest`.

### [ ] Step 4: Trip session state machine (recorder)
- Goal: decide start/end/resume of a trip from events, pure and testable.
- Key files: `core/TripRecorder.kt` (states Idle → Recording →
  GracePeriod → Recording; events: deviceConnected, deviceDisconnected,
  locationSample, manualStart, manualStop, graceTimerExpired), clock injected
  for tests.
- Tests: `TripRecorderTest.kt` — full session, disconnect+reconnect within grace
  minutes continues same trip, timeout ends trip, movement filters applied,
  manual start/stop.
- Validation: `./gradlew :app:testDebugUnitTest`.

## Phase 2 — Android plumbing

### [ ] Step 5: Location sampling
- Goal: feed location fixes to the recorder.
- Key files: `location/LocationSampler.kt` (thin wrapper over
  `LocationManager.requestLocationUpdates`, 30 s interval; forwards fixes),
  `location/LocationSource.kt` (interface so JVM tests fake it).
- Notes: default interval constant in one place. Fused provider NOT used in v1
  (plain LocationManager: no Play Services dependency, works on emulator).
- Validation: `:app:assembleDebug`.

### [ ] Step 6: Reverse geocoding
- Goal: coordinates → street + city; retry queue for failed lookups.
- Key files: `geocoding/GeocoderClient.kt` interface + Android `Geocoder`
  implementation (thoroughfare/subThoroughfare → street, locality → city),
  `geocoding/AddressParser.kt` (pure, unit-tested), pending-address retry job
  (run at app open + at report Generate).
- Tests: `AddressParserTest.kt` (address object → street/city mapping, missing
  fields).
- Validation: `:app:testDebugUnitTest`, `:app:assembleDebug`.

### [ ] Step 7: Settings + repositories + app wiring
- Goal: single place owning app state.
- Key files: `settings/SettingsRepository.kt` interface (monitoringEnabled,
  registered devices list, graceMinutes, sampling fixed constant),
  `settings/SettingsRepositoryImpl.kt` (SharedPreferences), `data/TripRepository.kt`
  (save finished trips, query range, delete, recent list),
  `TripLoggerApplication.kt` + simple manual DI container (no Hilt in v1).
- Notes: UI reads state through one `AppViewModel` per screen.
- Validation: `:app:assembleDebug`, tests pass.

### [ ] Step 8: Bluetooth monitoring + foreground service
- Goal: detect registered device connect/disconnect; run recorder continuously.
- Key files: `monitor/BluetoothMonitor.kt` (dynamic receiver for
  `ACTION_ACL_CONNECTED` / `ACTION_ACL_DISCONNECTED`, filters to registered
  devices), `monitor/TripMonitorService.kt` (foreground service, types
  connectedDevice + location, minimal notification, no cancel action,
  grace-period timer), wiring recorder → repository.
- Notes: BLUETOOTH_CONNECT + FOREGROUND_SERVICE permissions; runtime permission
  UI comes in Step 9/10. Confirm on-device: ACL broadcasts received for car
  head unit (some devices need profile connection instead — fallback discussed
  in Questions).
- Validation: `:app:assembleDebug`; manual on-device checks listed in step
  output.

## Phase 3 — UI

### [ ] Step 9: Home screen
- Goal: status card + today/yesterday trips with expand/delete.
- Key files: `ui/home/*` — state from a mapper (`UiStateMapper` pure function,
  unit-tested), status card states per UI.md (monitoring off / waiting / recording
  with device name or "Started manually", km, elapsed time), manual Start/Stop,
  recent trips list (today + yesterday), tap-to-expand detail, Delete button,
  "Address pending" display, "No trips yet" placeholder, notification permission
  request on first enable.
- Tests: `UiStateMapperTest.kt`.
- Validation: `:app:testDebugUnitTest`, `:app:assembleDebug`, manual emulator
  run.

### [ ] Step 10: Devices screen
- Goal: master monitoring switch, grace period picker (1–15 min, default 3),
  registered devices (add from paired list, remove, no duplicates).
- Key files: `ui/devices/*` — read paired devices
  (`BluetoothAdapter.bondedDevices`, requires BLUETOOTH_CONNECT permission),
  runtime permission flow, master switch gate (ON only with ≥ 1 registered),
  explainer texts per UI.md.
- Validation: `:app:assembleDebug`; manual run: register a device, remove it,
  toggle switch.

### [ ] Step 11: Report screen
- Goal: date range → preview → export CSV → share.
- Key files: `report/ReportCsvBuilder.kt` (pure, unit-tested: columns per
  todo.md, ISO dates, HH:mm, YYYY-MM month, chronological, totals row, UTF-8),
  `ui/report/*` (from/to pickers default last month, Generate, summary
  "12 trips · 386.4 km", preview list expandable, empty text, Export button →
  FileProvider file → ACTION_SEND share sheet), retry pending addresses before
  preview.
- Tests: `ReportCsvBuilderTest.kt` (headers, order, totals, escaping of commas/
  quotes/newlines).
- Validation: `:app:testDebugUnitTest`, `:app:assembleDebug`; manual: export +
  open in spreadsheet app.

## Phase 4 — release polish

### [ ] Step 12: End-to-end device validation + fixes
- Goal: verify real flow on a phone with the car.
- Manual checklist: pair car → register → enable monitoring → drive → auto
  trip saved → kill phone mid-trip → restart (partial discarded) → generate
  report → export → open in Sheets/Excel.
- Validation: all checklist items pass; fix defects found as separate small
  steps.

### [ ] Step 13: App icon + BUILD.md + doc sync
- Goal: custom launcher icon (design chosen by owner), BUILD.md with installable
  APK instructions (debug + release signing), README/UI.md/todo.md updated to
  match final behavior, plan.md completed.

## Questions answered

1. Bluetooth detection: **ACL broadcasts** (`ACTION_ACL_CONNECTED/DISCONNECTED`)
   — agreed. Profile-listener fallback only if real-device test fails.
2. Release signing: **debug APK only for now**; release signing documented when
   needed (Step 13 builds debug instructions; release optional).
3. Emulator **mock-location accepted** for early validation; real-driving test
   at Step 12.
4. **No migration infra until schema v2** exists; Room migrations added then.

## Open questions

- Bluetooth detection: rely on `ACTION_ACL_CONNECTED/DISCONNECTED`
   broadcasts (simple, works for most car units) — accepted? If the owner's car
   unit proves unreliable, fallback = listen on `BluetoothHeadset`/
   `BluetoothA2dp` profile connection states (needs profile support). Confirm
   default now, revisit at Step 8 device test.
- Release signing: installable APK for personal use only — debug build
   sufficient, or need release keystore + signed APK (BUILD.md covers both)?
- Fake location testing: unit tests use synthetic coordinates; real device
   validation needs actual driving. Emulator mock-location acceptable for early
   steps?
- Database on-device migration strategy: v1 no migrations needed (fresh
   installs); Room migration test added only when schema v2 appears — OK?
