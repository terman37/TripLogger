# TripToExcel — release notes

What the user sees, version by version. **Single source** for the Play Console's
"What's new" field: copy the block for the matching version as-is (no developer
jargon, no commit hashes — this is what users read before updating).

Two numbers, not to be confused:

- **Version code** (`versionCode` in `app/build.gradle.kts`): Play's internal
  counter. It must be **higher than everything already uploaded**, including to a
  testing track. It changes with every new AAB.
- **Version name** (`versionName`): what users see, on the store listing and in
  "About this app". It changes when the version really changes (1.0 → 1.1).

French version: [Français](../release-notes.md).

## 1.0 — 7 October 2026 (version code 2)

First published version.

- Automatic trip logging as soon as your car's Bluetooth connects: nothing to
  start before you drive.
- Reconnect grace period (1–15 minutes) so a brief Bluetooth dropout does not
  split one drive in two.
- Manual start/stop for trips that automatic detection misses.
- Excel (.xlsx) export for expense reports: dates, addresses, Google Maps links
  and a total row.
- French and English UI.
- Local data only: no account, no server, no ads, no analytics.

<!--
  Template for the next version — same tone, 1 to 5 bullets, user-visible changes
  first:

## 1.1 — (date) (version code 3)

- …
-->
