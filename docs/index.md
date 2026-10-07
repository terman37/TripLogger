# TripToExcel — documentation

Android app that **logs car trips automatically** when the car's Bluetooth
connects, and lets you **export them as an Excel spreadsheet** for your expense
report. Everything stays on the phone: no account, no server, nothing uploaded.

## Documents

- **[User guide](user-guide.md)** — installing, first-time setup, daily use,
  reports and spreadsheets, version and licence.
- **[Technical details](DETAILS.md)** — architecture, how a trip is recorded,
  data model, permissions, tests.
- **[Interface specification](UI.md)** — every screen, state and empty case.
- **[Building and installing](BUILD.md)** — debug build, signed release bundle.
- **[Privacy policy](privacy-policy.md)** — what the app does with your data
  (short answer: nothing leaves the phone).

## The app in one paragraph

Pair your car in Android's Bluetooth settings and register it in the app. When
the car connects, a trip starts; when it disconnects, the trip ends after a
configurable grace period so a brief Bluetooth dropout does not split one drive
in two. Distance, start/end addresses and times are recorded with the phone in
your pocket. Later you pick a date range and export a spreadsheet whose rows are
real Excel dates with Google Maps links and a total row.
