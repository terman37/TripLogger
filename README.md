# Trip Logger

Android application to log car trips automatically and export expense reports.

Trips are detected by **Bluetooth connection**: when a configured Bluetooth device
(the car) connects, a trip starts; when it disconnects, the trip ends. Each trip is
recorded with start/end timestamps, positions, reverse-geocoded addresses, and
distance in kilometers. Trips can be exported as a spreadsheet between two dates
for expense reports.

> Status: early development. The Gradle project skeleton exists; no feature code
> has been written yet. See [todo.md](todo.md) for the full feature list and
> [plan.md](plan.md) for the step-by-step implementation plan.

## Features

**Core (planned)**

- Background monitoring of Bluetooth connections for a configured device (the car)
- Auto start/stop of trip recording on connect/disconnect
- GPS position capture with reverse geocoding to get street addresses
- Distance calculation in kilometers
- Trip records stored in a local database:
  - start timestamp, start position, start address
  - end timestamp, end position, end address
  - number of kilometers
- Report generation between two dates
- Share/export of the report as a spreadsheet (CSV)

**Later / ideas (see todo.md)**

- Photos of tickets attached to trips (parking, tolls, fuel)
- Extraction of date / amount / debit account from ticket photos

## Architecture overview

```
┌─────────────────────────────────────────────────────────┐
│                      Android OS                         │
│   BluetoothAdapter / BluetoothDevice callbacks          │
│   LocationManager / GPS                                 │
└──────────────┬──────────────────────────┬───────────────┘
               │ connect/disconnect events │ location updates
┌──────────────▼──────────────────────────▼───────────────┐
│              Foreground Service (trip monitor)          │
│  - watches the configured Bluetooth device              │
│  - runs trip state machine: Idle → Recording → Ended    │
│  - samples location while recording                     │
└───────┬──────────────────────────────┬──────────────────┘
        │ trip start / end events      │ position samples
┌───────▼──────────┐        ┌──────────▼───────────┐
│  Trip Repository │        │ Geocoder (reverse)   │
│  (Room database) │        │  address lookup       │
└───────┬──────────┘        └──────────┬───────────┘
        │                              │
┌───────▼──────────────────────────────▼───────────────────┐
│  UI (single activity + fragments or Compose screens)     │
│  - trip list / current trip status                       │
│  - device selection                                      │
│  - report screen: pick date range → export spreadsheet   │
│  - share via Android Sharesheet / ACTION_SEND            │
└──────────────────────────────────────────────────────────┘
```

Key modules (planned):

| Module | Responsibility |
| --- | --- |
| `TripMonitorService` | Foreground service; listens for Bluetooth device connect/disconnect; starts/stops recordings |
| `TripRecorder` | State machine for a trip session; accumulates distance |
| `LocationTracker` | Requests GPS fixes at intervals; computes distances between fixes |
| `DistanceCalculator` | Haversine (or similar) formula for km between two coordinates |
| `GeocodingService` | Reverse geocodes coordinates into street addresses |
| `TripDao` / `TripDatabase` | Room entities and queries for trip rows |
| `ReportExporter` | Builds CSV/spreadsheet rows for a date range |
| `ReportViewModel` / UI | Date range picker, preview, export + share |

Design decisions and rationale:

- **Bluetooth as trip trigger** — no manual "start/stop" button needed; the car
  connection is the signal. Requires a background service because Bluetooth events
  must be observed even when the app is not in the foreground.
- **Foreground service with notification** — Android requires a foreground service
  (with visible notification) for long-running background work; also makes the
  active recording visible to the user.
- **Distance sampled while driving** — locations are sampled at intervals while
  the trip is active and small distances summed (see todo.md "Ideas"). Exact
  distance is not required for reporting, so sampling frequency is a tunable
  trade-off between battery life and precision.
- **Local Room database** — trips are personal data; no server, no account. Room
  gives typed queries and easy migration.
- **CSV export + Android Sharesheet** — the app generates a spreadsheet file and
  hands it to the system share sheet (`ACTION_SEND`), so the user can send it to
  any app (mail, drive, spreadsheet editor). This matches the open question in
  todo.md.

### How a trip is recorded

1. User pairs the car's Bluetooth device in the app (stores its MAC/name).
2. User starts the app's monitoring service (persistent notification).
3. Car connects → service wakes → trip starts:
   - record start timestamp and GPS position
   - reverse-geocode position → start address
4. While connected, positions are sampled; distance accumulates.
5. Car disconnects → trip ends:
   - record end timestamp, position, address, total km
   - insert one row in the database
6. Later, the user picks two dates → app lists trips in range → export CSV →
   share.

### Permissions (planned, subject to Android version rules)

| Permission | Purpose |
| --- | --- |
| `BLUETOOTH_CONNECT` (runtime) | Observe connection state of the car device |
| `ACCESS_FINE_LOCATION` (runtime) | GPS fixes for start/end position and distance |
| `FOREGROUND_SERVICE` + location/connected-device types | Run monitoring service |
| `POST_NOTIFICATIONS` (runtime) | Foreground service notification |

No storage permission is needed: the database and export files live in
app-internal / app-specific storage and are shared through the system share sheet.

## Project structure

```
app/src/main/java/com/terman37/triplogger/
├── MainActivity.kt              # entry point
├── data/                        # Room entities, DAO, database
├── monitor/                     # Bluetooth monitor, trip recorder, state machine
├── location/                    # location sampling, distance, reverse geocoding
├── report/                      # report query + CSV export
└── ui/                          # screens: trips, device setup, report
```

Code targets **Kotlin**, minSdk 34 (Android 14), targetSdk 37. See
[BUILD.md](BUILD.md) for building an installable APK.

## Testing

Unit tests (`app/src/test/...`) cover pure logic: distance calculation, trip
state machine, CSV formatting, report date-range queries (Room in-memory). Where
Android APIs are involved, logic is isolated behind interfaces so tests run on
the JVM without an emulator.

## Documentation

- [todo.md](todo.md) — user feature list and open questions
- [plan.md](plan.md) — step-by-step implementation plan (checkboxes)
- [AGENTS.md](AGENTS.md) — conventions for AI agents / contributors working in this repo
- [BUILD.md](BUILD.md) — how to produce an installable file
