# Trip Logger — UI specification

Current description of how the app looks and behaves. Reflects the validated
implementation (the validated implementation); keep in sync with the code.

## Global decisions

| Item | Decision |
| --- | --- |
| UI toolkit | Jetpack Compose |
| Design system | Material 3, dark theme only |
| Accent palette | mint green `#41D8B7` (primary), sky blue `#7AB8FF` (secondary), amber `#FFD166` (tertiary: map icons, grace) on dark neutrals; deep red `#FF5252` (`DestructiveRed`) for delete actions |
| Language | English only |
| App structure | Single activity, bottom navigation, 3 tabs |
| Tab order | **Home / Report / Devices** |
| Icon | Adaptive: dark background, S-shaped mint road with dark centre stripe + warm amber car; S road + car silhouette as themed-icon layer |

## Screen 1 — Home

Top to bottom:

1. **Monitor trips switch** — master switch with explainer. Disabled (with
   hint "Register a device…") until at least one device is registered. Turning
   it ON requests the three runtime permissions (Bluetooth, location,
   notifications) the first time; grants enable monitoring immediately.
2. **Status card** — one of:

| Condition | Card |
| --- | --- |
| Monitoring off | "Monitoring off" + hint to use the switch |
| Monitoring on, idle | "Waiting for {device}…" (or "one of your registered devices"); **Start manually** button |
| Recording (auto) | "Recording", "Connected: {device}", live km, elapsed time, **Stop** |
| Recording (manual) | Same, "Started manually" instead of device |
| Grace period | "Disconnected", "Finishing trip — reconnect to resume.", km, "Disconnected since X min" (live), **Stop now** |

3. **Recent trips** (heading) — trips started **today or yesterday**, newest
   first. Empty state: "No trips yet".

Collapsed row: date + start time ("Aug 6, 14:32"), summary ("Paris → Lyon"),
amber **map icon** opening Google Maps **directions** between start and end
(hidden when neither end has any location), distance, expand chevron.

Expanded: time range, duration, then From and To address lines. Each address
line has its own amber **pin icon** opening that single place in Google Maps
(hidden when it has neither address nor coordinates). The To line carries a red
**trash icon**: it opens a confirmation dialog ("Delete this trip?") and only
then removes the trip (destructive buttons styled `DestructiveRed`).

## Screen 2 — Report

1. **From / To filters side by side** at the top (Material date pickers).
   Default range: the **last 7 days including today**. Picking a From after To
   (or vice versa) clamps the other end.
2. **Trip list always visible** for the selected range — no "Generate" button;
   changing a date reloads the list. Above it a summary line
   "N trips · X km". Empty state: "No trips in this period".
3. **Footer** (bottom, full width): a filled **red trash button** far left and
   "Export spreadsheet" far right.
   - Red trash button: deletes **all trips in the current range** after a
     confirmation dialog naming the count and date range; disabled when the
     range is empty; the list reloads afterwards.
   - Export: retries pending addresses, builds the CSV, opens the Android share
     sheet; the preview refreshes so newly geocoded addresses appear.

Preview rows use the same collapsed/expandable card as Home, **including the
per-trip red trash icon + confirmation** (deleting from the report refreshes the
list).

## Screen 3 — Devices

Both device sections are **collapsible** (tap the header): the header shows the
section name and the current device count. Registered starts expanded;
Available starts collapsed so the page stays short.

1. **Registered** — devices that trigger a trip on connect. Each row: name +
   MAC, **X** to remove. Empty: "No device registered".
2. **Available** — devices paired in Android settings, **+** to register.
   Missing Bluetooth permission → "Allow Bluetooth access" button (grants
   `BLUETOOTH_CONNECT` and refreshes). No in-app pairing by design.
3. **Reconnect grace period** — slider 1–15 minutes (default 3), persisted.

## Background notification

- Title "Trip Logger"; content always leads with **"Monitoring active"**:
  - idle → "Monitoring active"
  - recording → "Monitoring active — Recording 12.4 km"
  - grace → "Monitoring active — Disconnected, finishing trip…"
- Ongoing, no action buttons; tap opens the app.
- If the user dismisses it (allowed on recent Android), monitoring stops and
  any running trip is finished/saved — the notification is the monitoring
  indicator, silently running without it is not allowed.

## Trip rules (behavior visible in the UI)

- Detection: every 10 s the registered devices' connection state is polled via
  Bluetooth profile proxies (A2DP/HEADSET/LE_AUDIO). ACL broadcasts are not
  delivered on Android 14+ (found on device).
- Grace period: a disconnect finishes the trip only after the configured
  minutes; reconnecting inside the window resumes the same trip.
- Trips shorter than **50 m** are discarded (parked-engine noise); they never
  appear as rows.
- Manual Start/Stop works without a device; manual trips ignore Bluetooth.
- Kill/force-stop during a trip discards the partial trip (KISS).
- Distance: GPS every 30 s, keep fixes moving ≥ 10 m with accuracy ≤ 50 m and
  implied speed ≤ 160 km/h; sum of Haversine steps.
- Addresses: resolved at trip end when online; failures show "Address pending"
  and are retried when the Report screen opens and before export.

## CSV export columns

`start date (ISO) | start time (HH:mm) | start month (YYYY-MM) | end date |
end time | start city | start address | end city | end address | km (1 decimal,
dot) | start maps link | end maps link` — chronological, UTF-8, RFC-4180
escaping. Final `Total` row: "Total" in the first column and the km sum **under
the km column** (maps-link columns empty).

## Empty / first-run states

| Place | State | Text |
| --- | --- | --- |
| Home list | no trips today/yesterday | "No trips yet" |
| Home switch | no registered device | "Register a device (Devices tab) to enable auto-recording." |
| Devices registered | empty | "No device registered" |
| Devices available | permission missing / nothing paired | "Allow Bluetooth access to see paired devices." / "No paired devices. Pair in Android settings, then come back." |
| Report list | no trips in range | "No trips in this period" |
