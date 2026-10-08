# TripToExcel — documentation

Android app that **logs car trips automatically** when the car's Bluetooth
connects, and lets you **export them as an Excel spreadsheet** for your expense
report. Everything stays on the phone: no account, no server, nothing uploaded.

## Documents

- **[User guide](user-guide.md)** — installing, first-time setup, daily use,
  reports and spreadsheets, version and licence.
- **[Technical details](../DETAILS.md)** — architecture, how a trip is recorded,
  data model, permissions, tests.
- **[Interface specification](../UI.md)** — every screen, state and empty case.
- **[Building and installing](../BUILD.md)** — debug build, signed release bundle.
- **[Privacy policy](privacy-policy.md)** — what the app does with your data
  (short answer: nothing leaves the phone).
- **[Release notes](release-notes.md)** — what changes in each version (source for
  the Play listing's "What's new").

## The app in one paragraph

Pair your car in Android's Bluetooth settings and register it in the app. When
the car connects, a trip starts; when it disconnects, the trip ends after a
configurable grace period so a brief Bluetooth dropout does not split one drive
in two. Distance, start/end addresses and times are recorded with the phone in
your pocket. Later you pick a date range and export a spreadsheet whose rows are
real Excel dates with Google Maps links and a total row.

## Support

If the app is useful to you, you can buy me a Ko-fi. It is entirely optional:
nothing in the app is locked behind it, and there are no ads and no paid tier.
The link opens ko-fi.com in your browser.

[![Support me on Ko-fi](../assets/img/support_me_on_kofi_badge_blue.png)](https://ko-fi.com/anthonyjourdan)

