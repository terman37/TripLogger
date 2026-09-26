# Trip Logger — technical details

Technical documentation for developers and the curious. User guide:
[README.md](README.md) · Interface spec: [UI.md](UI.md) · Builds:
[BUILD.md](BUILD.md) · Backlog: [todo.md](todo.md)

## What the app does

Android app (Kotlin, Jetpack Compose) that logs car trips automatically on
Bluetooth connection and exports them as an Excel spreadsheet. Local only: no
server, no account, no data leaves the phone.

Validated on a real device (Pixel 9a, Android 17).

## Architecture overview

```
┌────────────────────────────────────────────────────────────────┐
│  UI — single activity, Compose, bottom nav (Home/Report/Devices)│
│  HomeViewModel / ReportViewModel / DevicesViewModel            │
└───────────────┬────────────────────────────────────────────────┘
                │ StateFlow / suspend calls (AppContainer singletons)
┌───────────────▼────────────────────────────────────────────────┐
│  Domain & data                                                 │
│  TripRecorder (state machine)   TripRepository (Room + geocode)│
│  LocationFilter + DistanceCalculator   SettingsRepository      │
│  AddressParts, MapsUrl          PendingAddresses               │
└───────────────┬────────────────────────────────────────────────┘
                │ Android adapters (interfaces kept thin)
┌───────────────▼────────────────────────────────────────────────┐
│  TripMonitorService (foreground service = the engine's host)   │
│  BluetoothMonitor  · LocationManagerLocationSource             │
│  AndroidGeocoderClient · Room/SQLite · FileProvider (share)    │
└────────────────────────────────────────────────────────────────┘
```

Package layout (`app/src/main/java/com/terman37/triplogger/`):

| Package | Contents |
| --- | --- |
| `core/` | Pure Kotlin, JVM-tested: `TripRecorder` (Idle → Recording → Grace), `LocationFilter`, `DistanceCalculator` (Haversine), `TrackingPolicy` (fixed constants), `AddressParts`, `MapsUrl`, `TripOrigin`, `TripDraft`, `Clock` |
| `data/` | Room: `Trip` entity, `TripDao`, `TripDatabase`; `TripRepository` (save + geocode + retry), `PendingAddresses`, `RegisteredDevice` |
| `settings/` | `SettingsRepository` + SharedPreferences impl (monitoring switch, grace minutes, registered devices) |
| `geocoding/` | `GeocoderClient` interface + Android `Geocoder` implementation |
| `location/` | `LocationSource` interface + `LocationManager` implementation |
| `monitor/` | `TripMonitorService` (foreground service), `BluetoothMonitor` (polling), `PairedDevicesSource` impl, `NotificationDismissReceiver` |
| `report/` | `ReportXlsxBuilder` (pure, .xlsx) |
| `ui/` | `MainActivity`, `TripLoggerApp` (nav), `home/`, `report/`, `devices/`, `common/` (TripRowCard, MapsUrl usage), `theme/` |

`AppContainer` is a manual DI singleton created by `TripLoggerApplication`; it
owns the single shared `TripRecorder` so the service and the UI see the same
state.

## Data model (`data.Trip`)

One row per trip: `startEpochMillis`, `startLat/Lng` (nullable), `startStreet/
startCity` (nullable = address pending), the same four for the end,
`distanceKm` (Double, 1 decimal displayed), `origin` (`AUTO`/`MANUAL`).

## How a trip is recorded

1. **Detection** — every 10 s `BluetoothMonitor` polls the connection state of
   the registered devices through Bluetooth profile proxies (A2DP, HEADSET,
   LE_AUDIO) and diffs against the previous poll.
   *Why polling:* the classic `ACTION_ACL_CONNECTED/DISCONNECTED` broadcasts
   are not delivered to this app on Android 14+ (found on a real device), and
   there is no per-device connection-state API.
   *Startup rule:* a device already connected when monitoring is enabled counts
   as a connect event (trip starts immediately).
2. **Trip starts** (auto) — recorder enters RECORDING; start timestamp is now.
3. **GPS** — `LocationManager` GPS provider, one fix every 30 s while
   RECORDING (`TrackingPolicy`). `lastKnown()` anchors the trip immediately.
   *Filters* (`LocationFilter`): keep a fix only when it moved ≥ 10 m from the
   last kept fix, accuracy ≤ 50 m, and the implied speed is ≤ 160 km/h. Distance
   = sum of Haversine steps of kept fixes. Parked cars and GPS jumps therefore
   add nothing.
4. **Disconnect** — recorder enters GRACE (configurable 1–15 min, default 3).
   Reconnect within the window resumes the same trip; otherwise the grace timer
   in the service calls `onGraceTimerExpired()` and the trip finishes.
   The timer is self-healing: it re-checks `isActive`, releases its slot in
   `finally`, and re-schedules for the remaining time if a later evaluation
   finds the grace state without an active timer (bug fixed after flapping
   car connections).
5. **Finish** — a `TripDraft` is produced and `TripRepository.saveTrip()`
   geocodes both endpoints (online) and inserts the row. Trips shorter than
   **50 m** (`TrackingPolicy.MIN_TRIP_DISTANCE_KM`) are discarded: parked-engine
   sessions are noise.
6. **Manual fallback** — Home "Start manually" starts a trip without a device;
   it ignores Bluetooth events and ends on "Stop".
7. **Process death** — force-stop during a trip discards the partial trip (no
   persistence of in-progress state; deliberate KISS decision). A reboot
   discards it too, but `BootCompletedReceiver` restarts the monitoring service
   afterwards (see "Notification = monitoring indicator"), so monitoring
   resumes without opening the app.

### Notification = monitoring indicator

`TripMonitorService` is a foreground service with a persistent notification
(types `connectedDevice|location`), text "Monitoring active[ — Recording x km |
— Disconnected, finishing trip…]". If the user dismisses the notification
(allowed on recent Android versions), `NotificationDismissReceiver` (deleteIntent)
turns monitoring off, finishes and saves the running trip, and stops the
service — the app never records silently. Tap opens the app.

The service is started/stopped by the Home master switch; `START_STICKY` and
`evaluate()` (state → foreground/BT/GPS/grace/stop) make restarts idempotent.
A reboot or an app update kills the service without touching the persisted
switch, so `BootCompletedReceiver` starts it again when monitoring is ON and at
least one device is registered. Android 14+ refuses to start a location
foreground service from the background unless the app holds
`ACCESS_BACKGROUND_LOCATION`, so monitoring requires location "Allow all the
time" (the Home dialog links to the system settings page). If that start cannot
run (OEM autostart block, revoked type permission, background location denied),
the receiver and the service switch monitoring OFF instead of leaving the UI
showing "on" while nothing runs — the same never-silent rule as the notification
dismissal.

## Addresses and lazy retry

Reverse geocoding uses the platform `Geocoder` (no Play Services). At trip end
it fills street/city; offline it leaves them null ("Address pending" in the
UI). `TripRepository.retryPendingAddresses()` fills missing sides later, and is
called when the Report screen opens and again before an export.

## Excel export

`ReportXlsxBuilder` (pure, unit-tested) writes a minimal Excel `.xlsx` workbook
(Office Open XML: a ZIP of hand-written XML parts; Apache POI is too large and
not Android friendly). Columns:

| # | Column |
| --- | --- |
| 1 | start date (real Excel date/time, displayed `dddd d mmmm, hh:mm`) |
| 2 | end date (same) |
| 3 | start address (`street, city`, hyperlink to Google Maps) |
| 4 | end address (same) |
| 5 | km (numeric, 1 decimal) |
| 6 | trip (hyperlink to Google Maps directions start → end) |

Dates are true Excel date/time values (numeric serials), so they sort, compare
and can be reformatted with Format Cells; Excel renders the weekday and month
names in its own language. The header row is light gray, bold and frozen; all
cells have 0.75 pt solid black borders. A
missing address shows `Address not found`, still linked when the endpoint has
coordinates. The last row `Total` holds `=SUM(...)` over the km column (so
deleting a row in Excel keeps the total correct) on a light-yellow background. Files are written to `cacheDir/exports/` as
`trips_<from>_<to>.xlsx` and shared through a `FileProvider` URI +
`ACTION_SEND` (the Android share sheet).

## Google Maps links (`core.MapsUrl`)

Pure URL builder with two link kinds, each preferring a street address over
the raw coordinates (a city-only address would only zoom to the city):
`directions` (route between both endpoints; used by the map icon on the
collapsed trip row) and `place` (single endpoint; used by the pin icons next to
the From/To addresses in the expanded detail and by the export hyperlinks).
Null when an endpoint has neither address nor coordinates → UI hides the icon,
the export writes plain text without a link.

## UI and theme

Screens and states are specified in [UI.md](UI.md). Compose Material 3, dark
theme only: mint `#41D8B7` (primary), sky blue `#7AB8FF` (secondary), amber
`#FFD166` (tertiary, map icons / grace), plus `DestructiveRed` `#FF5252` for
delete actions (the Material error pink was too pale for the user).

## Permissions

| Permission | Why |
| --- | --- |
| `BLUETOOTH_CONNECT` | Read registered device names, poll connection state |
| `ACCESS_FINE_LOCATION` | GPS fixes while recording |
| `POST_NOTIFICATIONS` | Foreground-service notification |
| `FOREGROUND_SERVICE` + `CONNECTED_DEVICE`/`LOCATION` | Run the monitor |

No storage permission: the database and exports live in app-private storage.

## Testing

- JVM unit tests (`app/src/test`, 111 tests):
  - geo/math: Haversine, `LocationFilter` (displacement/accuracy/speed), address
    parsing, `PendingAddresses`, `MapsUrl` encoding + fallbacks;
  - state machine: `TripRecorder` full lifecycles (grace, reconnect, manual,
    discard threshold, no-GPS, wrong-state events);
  - detection rules: `ConnectionDiff` (first poll, connect, disconnect, swap);
  - settings rules: grace clamping, device add/remove/dedupe;
  - report: `ReportDates` defaults/range/zone, `ReportXlsxBuilder` columns,
    hyperlinks, styles, totals under the km column, true Excel dates, timezone;
  - UI mapping: Home and Devices state mappers;
  - integration (`TripRecordingFlowTest`): recorder → repository → Room (fake)
    → geocoded addresses → Excel, including offline-pending + lazy retry, grace
    reconnect keeping one trip, manual origin and the <50 m discard.
- Instrumented tests (`app/src/androidTest`, run on a device):
  - Room DAO CRUD/query round-trips;
  - `NotificationFactoryTest`: every monitoring state produces a valid,
    postable notification (catches the "Invalid notification (no valid small
    icon)" crash class) and the text always leads with "Monitoring active".

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

## Building

See [BUILD.md](BUILD.md) (debug install, optional signed release, versioning,
troubleshooting).
