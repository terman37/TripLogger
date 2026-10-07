# TripToExcel

An Android app that **logs your car trips automatically** and lets you export
them as a spreadsheet for your expense report.

When your car's Bluetooth connects, the trip starts; when it disconnects, the
trip ends. Distance, start/end addresses and times are recorded while you drive.
Later you pick a date range and export an Excel file you can send to your
employer or accountant.

Everything stays **on your phone**: no account, no server, nothing uploaded.

- Automatic trip logging over Bluetooth, with a reconnect grace period so a
  short dropout does not split one drive in two
- Manual start/stop for the rare trip that detection misses
- Excel (`.xlsx`) export with real Excel dates, addresses and Google Maps links
- **Local-only**: no analytics, no ads, Android's own backup disabled
- Free software under the GPL-3.0-or-later, minimum Android 14 (API 34)

## Documentation

**Full documentation: <https://terman37.github.io/TripLogger/>**

| Document | What it covers |
| --- | --- |
| [User guide](docs/user-guide.md) | Installing, first-time setup, daily use, reports |
| [Technical details](docs/DETAILS.md) | Architecture, how a trip is recorded, tests |
| [Interface specification](docs/UI.md) | Every screen, state and empty case |
| [Building and installing](docs/BUILD.md) | Debug build, signed release bundle |
| [Privacy policy](https://terman37.github.io/TripLogger/privacy-policy.html) | What the app does with your data |

Those files also live in [`docs/`](docs/) and are published as a website by
GitHub Pages (source: `docs/`, Markdown rendered with Jekyll).

## License

TripToExcel is free software, released under the **GNU General Public License,
version 3 or later** (`GPL-3.0-or-later`).

Copyright (C) 2026 Anthony Jourdan.

You may use, study, share and modify the app. If you distribute it or a version
derived from it, you must publish the corresponding source under the same
license. Full terms: [LICENSE](LICENSE).
