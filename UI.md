# Trip Logger — UI specification

Precise description of how the app looks and behaves. Written before coding:
the plan and implementation follow this document.

## Global decisions

| Item | Decision |
| --- | --- |
| UI toolkit | Jetpack Compose |
| Design system | Material 3, default color palette |
| Theme | Dark only (no light mode, no toggle) |
| Accents | Google Maps link buttons in tertiary; destructive Delete actions in vivid red (`DestructiveRed`, #FF5252) |
| Language | English only |
| App structure | Single screen container with bottom navigation bar, 3 tabs |
| App icon | Custom icon (design + assets to be defined; default icon until then) |

Bottom navigation tabs (always visible, order fixed):

1. **Home**
2. **Devices**
3. **Report**

## Screen 1 — Home

### Top: status card

One card whose content depends on two states: monitoring master switch and
current recording state. A **recorded trip** here means: a trip is currently
being tracked (started automatically by a registered device or manually).

| Monitoring | Recording | Appearance | Content |
| --- | --- | --- | --- |
| OFF | — | Gray card | Title "Monitoring off", hint text, button **Enable monitoring** (jumps to Devices) |
| ON | No car, idle | Neutral card | Title "Waiting for car", shows registered device name, e.g. *"Waiting for Car bluetooth…"*, button **Start manually** |
| ON | Recording (auto) | Card tinted (accent) | Title "Recording", line 1: connected device name (e.g. *"Connected: Car bluetooth"*), line 2: live distance in km (e.g. *"12.4 km"*), line 3: elapsed time (e.g. *"00:42"*), red button **Stop** |
| ON | Recording (manual) | Same as above | Device line reads *"Started manually"* instead of connected device |

Notes:
- **Start manually** = fallback when the Bluetooth trigger missed (e.g. engine
  started before phone connected). Starts tracking without a device.
- **Stop** ends the current trip, stores it in the database like an auto trip.
- Elapsed time updates every second; distance updates on each GPS sample.

### Below: recent trips

List of trips from **today and yesterday** (only those two days; newest first).
While no trips exist on either day, show placeholder text "No trips yet".

Each collapsed row shows:

- date + start time (e.g. *"Aug 6, 14:32"*)
- start → end address summary (e.g. *"Home → Office"*; if address missing,
  fall back to "Start → End")
- distance (e.g. *"12.4 km"*)
- directions icon button (opens a Google Maps route start → end); hidden when
  neither endpoint has address or coordinates

Tap a row → expands in place (no separate screen), showing:

- start time and end time (e.g. *"14:32 – 15:15"*)
- trip duration
- start address (full line), preceded by an icon button that opens that place
  in Google Maps
- end address (full line), preceded by the same icon button
- distance
- red trash **Delete** icon on the right of the "To" row (removes the trip from
  the database; shares the row, no extra line)

No edit of trip data in the UI (v1).

Addresses that could not be reverse-geocoded (offline at trip end) show as
*"Address pending"* and are retried automatically next time the app opens and
again when a report is generated.

## Screen 2 — Devices

Below the page title **"Bluetooth devices"**:

1. **Master switch row** — label "Monitor trips", switch. Description line under
   it: explains it records a trip whenever a registered device connects.
2. **Reconnect grace period row** — label "Reconnect grace period", value picker
   (minutes, 1–15, default 3). Description: a Bluetooth disconnect only ends a
   trip after this many minutes; reconnecting within the period resumes the same
   trip. Guards against flaky car connections.
3. **Registered devices** section — collapsible header "Registered (N)" with an
   expand/collapse chevron; starts expanded. Helper line "Devices that trigger a
   trip when they connect." stays visible when collapsed. Each row: device name
   + **X** remove button. If empty: "No device registered".
4. **Paired devices** section — collapsible header "Available (N)"; starts
   collapsed to keep the page short. Helper line "Devices paired in Android
   settings." stays visible when collapsed. Rows: all devices paired in Android
   settings, each with a **+** add button. Devices already registered are not
   listed here (no duplicates).

Behavior:
- **+** on an available device moves it into Registered.
- **X** removes it from Registered (device stays paired in Android).
- No in-app pairing/scanning: pairing happens in Android settings.
- Master switch OFF → no automatic trip start even if a registered device
  connects. Manual Start on Home still works.
- Master switch can only be turned ON when at least one device is registered —
  otherwise show a hint.
- GPS sampling interval (30 s) is fixed in code in v1, not configurable here.

## Screen 3 — Report

Top to bottom:

1. **Date range row** — two fields, "From" and "To", default = **last 7 days
   incl. today**. Tap a field opens a Material date picker dialog. The list
   reloads live on every change (no Generate button).
2. **Summary line** — e.g. *"12 trips · 386.4 km"*.
3. **Preview list** — one row per trip in range (oldest first): date, start → end
   address, km. Tap row → expand (same detail as Home, including the red trash
   delete). Empty range → *"No trips in this period"*.
4. **Footer actions** — a filled red button with a white trash icon (same
   footprint as Export) deletes ALL trips shown for the current range (after a
   confirmation dialog); "Export spreadsheet" on the right generates the CSV
   and opens the share sheet with the file attached.

## Background notification

While the monitoring service runs, a persistent notification is shown
(required by Android for foreground services; gives visibility that tracking is
active):

- Minimal content: title "Trip Logger", text "Monitoring active"
- While a trip is recording, text: "Recording — 12.4 km" (content stays minimal,
  no buttons)
- No cancel/stop action on the notification
- Tapping the notification opens the app

## Empty / first-run states summary

| Place | State | Text |
| --- | --- | --- |
| Home recent list | no trips today/yesterday | "No trips yet" |
| Home status card | monitoring off | "Monitoring off" + enable button |
| Devices registered | empty | "No device registered" |
| Report preview | no trips in range | "No trips in this period" |
