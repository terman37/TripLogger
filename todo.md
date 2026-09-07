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
    end address / km (1 decimal, dot); chronological; totals row; UTF-8
  - Address stored as street + city separately (reverse geocoder returns
    structured data)
  - Reports contain trip rows only; no cost fields in v1

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

## Open questions

None for v1 core scope — UI (UI.md) and behavior decisions are settled.
Remaining work is implementation, tracked in plan.md.
