# Trip Logger — todo

## Done

- [x] Create Android project skeleton (Gradle, Kotlin, minSdk 34 / targetSdk 37)
- [x] Write README.md: architecture + how the application works
- [x] Write AGENTS.md: project conventions for agents/contributors
- [x] Settle core architecture questions (details in README):
  - Background monitoring: yes, via a foreground service with notification
  - Permissions: BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION, POST_NOTIFICATIONS,
    FOREGROUND_SERVICE (+ types); no storage permission needed
  - Share mechanism: app generates the spreadsheet file, then uses the Android
    share sheet (ACTION_SEND)
  - Distance: sample GPS while driving and sum small distances; no offline OSM
    recalculation afterwards (not needed for reporting)
  - Data storage: local Room database, no server/account

## Core features (backlog)

- [ ] Detect when the configured Bluetooth device connects → start recording trip
- [ ] Get start address (reverse geocoding)
- [ ] Detect when it disconnects → end trip
- [ ] Get end address
- [ ] Compute distance in km
- [ ] Write row in db:
  - [ ] start timestamp
  - [ ] start position
  - [ ] start address
  - [ ] end timestamp
  - [ ] end position
  - [ ] end address
  - [ ] number of kilometers
- [ ] Generate a report between two dates
- [ ] Share the report

## Process / instructions

- [ ] Create plan.md: step-by-step implementation plan with checkboxes
  - each step small enough to review and commit separately
  - update plan after each step
- [ ] Create BUILD.md: instructions to generate the installable APK
- [ ] Add beginner-friendly comments in the code while implementing
- [ ] Code should have tests (unit + instrumented)

## Open questions — not decided yet

On **how the app should look**:

- UI toolkit: Jetpack Compose vs XML layouts? (First Android app; Compose is the
  modern default, XML more classic. Not decided.)
- Screens needed: trip list, live recording status, Bluetooth device picker,
  report/export screen? Main screen layout?
- Should the app show a manual "start/stop trip" fallback button (when BT trigger
  fails), or rely 100% on Bluetooth?
- Dark/light theme, language (English only?), app icon?

On **how the app should work**:

- How does the user select the car's Bluetooth device? (Assumption in README:
  pick from paired devices list, stored in settings.)
- Trip edge cases: engine on but car parked (no movement), traffic stops,
  GPS signal lost mid-trip, app force-stopped during a trip → how to recover?
- GPS sampling: what interval is a good battery/precision trade-off? Filter
  inaccurate fixes / jitter while stopped?
- Reverse geocoding needs network. No network at trip end → store position only
  and geocode later? Offline geocoding option?
- CSV format details: column order, date format, decimal separator / encoding for
  spreadsheet compatibility (Excel, Google Sheets, LibreOffice)?
- Is 1 decimal on km enough? Do addresses need to be one column or split
  (street / city)?
- Distance per trip or cumulative daily? Any trip editing (manual fix of
  address/distance) before export?
- Do expense reports need more than trip rows (e.g. per-trip cost field, or a
  daily total sheet)? Currently no cost data is modeled.
