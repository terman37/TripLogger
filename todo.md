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
- [x] Define the UI precisely (see UI.md):
  - Jetpack Compose, Material 3 default palette, dark theme only, English
  - Bottom navigation with 3 tabs: Home / Devices / Report
  - Home: status card (monitoring state + live recording info + manual
    start/stop fallback), recent trips of today/yesterday, tap-to-expand
    detail with Delete (no edit)
  - Devices: master "Monitor trips" switch, registered devices list with
    remove, paired devices list with add; no in-app pairing
  - Report: date range picker (default last month), Generate, summary + preview,
    Export → Android share sheet
  - Background notification: minimal content, no cancel action
  - Custom app icon to be designed later
- [x] Settle behavior questions (details in UI.md + README):
  - Reconnect grace period: BT disconnect ends trip only after N minutes;
    reconnect within period continues same trip; configurable on Devices page
    (1–15 min, default 3)
  - GPS sampling: 30 s interval, keep fixes only if moved ≥ 10 m and accuracy
    ≤ 50 m, drop steps implying > 160 km/h; distance = sum of Haversine steps
  - Sampling interval fixed in code (not configurable) for now
  - Manual Start records a trip without a device (fallback)
  - Reverse geocoding: attempt at trip end; on failure store coordinates with
    "address pending"; retried lazily on app open and at Report Generate
  - Trip recovery after phone restart/force-stop: discard partial trip (KISS)
  - GPS gaps mid-trip accepted: distance slightly under, OK for reporting
  - CSV columns: start date (ISO) / start time (HH:mm) / start month (YYYY-MM) /
    end date (ISO) / end time / start city / start address / end city /
    end address / km (1 decimal, dot) / start maps link / end maps link
    (Google Maps search URLs); chronological; totals row; UTF-8
  - Address stored as street + city separately (reverse geocoder returns
    structured data)
  - Reports contain trip rows only; no cost fields in v1
- [x] Recording engine implemented (plan Steps 2–8): Room schema/DAO, distance+filters, recorder state machine with grace period, GPS sampling, geocoding with lazy retry, settings + repository + DI, Bluetooth ACL monitoring + foreground service

## Core features (backlog)

- [x] Trip logging engine — detect registered Bluetooth connect/disconnect,
  start/end trip, reverse-geocoded addresses, distance in km, database row
  with all fields (implemented in plan Steps 2–8). Becomes usable once the
  Home/Devices UI lands (Steps 9–10): until then nothing can enable it.
- [ ] Generate a report between two dates (Step 11)
- [ ] Share the report as spreadsheet (Step 11)

## Later / additional features

- [ ] Store picture of tickets with date
- [ ] Extract information from ticket picture:
  - [ ] date
  - [ ] value
  - [ ] debited account

## Process / instructions

- [x] Create plan.md: step-by-step implementation plan with checkboxes
  - each step small enough to review and commit separately
  - update plan after each step
- [ ] Create BUILD.md: instructions to generate the installable APK
- [x] Add beginner-friendly comments in the code while implementing
- [x] Code should have tests (unit + instrumented; DAO tests need a device: connectedDebugAndroidTest not yet run)

## Open questions

None for v1 core scope — UI (UI.md) and behavior decisions are settled.
Remaining work is implementation, tracked in plan.md.

## Project documentation (to do)

- [ ] Refresh README.md + UI.md to match the implemented code. README
  "Status" section still claims "no feature code written yet" — stale since
  Step 1. Update architecture notes (package layout changed: core/, data/,
  settings/, geocoding/, location/, monitor/, ui/) and record final
  behaviors. Planned as part of plan Step 13, but an interim pass after the
  UI steps is welcome.

