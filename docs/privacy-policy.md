# TripToExcel — Privacy Policy

*Last updated: 2 October 2026*

> **TripToExcel is a local-only app.** It does not collect, transmit, or sell your
> personal data. There is no account, no server, and no analytics. Your trips stay
> on your phone unless you choose to share an export yourself.

## 1. Who this policy is for

This policy covers the Android app **TripToExcel**, listed on Google Play as
*TripToExcel - Km Logger* (package `com.terman37.triplogger`), developed by
**Anthony Jourdan**. It explains
what the app uses on your device, where that data goes, and how you can remove it.

## 2. What the app uses on your device

TripToExcel accesses the following data *on your device only*:

- **Bluetooth connection status** of the car device you register in the app. This
  is used solely to know when a trip starts and when it ends. The app does not
  read or transfer audio, contacts, or files over Bluetooth.
- **Location (GPS)**, while a trip is recording, to calculate the driven distance
  and to resolve the start and end addresses.
- **Trip records** derived from the data above: start and end time, start and end
  coordinates, street and city names, and distance.
- **Notifications**, to display the ongoing “Monitoring active” notification that
  shows when the app is recording.

## 3. No collection, no transmission

TripToExcel does not send your data anywhere. It has no backend server, no user
account, no advertising SDK, and no analytics or crash-reporting service.

The only way trip data leaves your phone is when **you** explicitly export a
spreadsheet and share it through the Android share sheet (for example by email or
to cloud storage). That action is entirely under your control.

## 4. Where the data is stored

Trip records are stored in the app's private storage on your phone (a local
database, plus a temporary export folder in the app's private cache). Other apps
on your phone cannot read this storage. Exported spreadsheets are shared only
through the Android share sheet, and only when you start the export.

Android's own backup is switched off (`allowBackup="false"`), so trip data is not
copied to your Google Drive or to another phone by the system.

## 5. Deleting your data

- Delete a single trip from the trip list or the report list.
- Delete all trips in a selected date range from the Report tab.
- Uninstall the app to remove all stored trip data from your phone.

Because the developer never receives your data, there is nothing to delete on a
server; deletion is entirely local and immediate.

## 6. Third parties

- **Android's built-in geocoder** is used to turn coordinates into an address.
  This is a system service; its handling of data is governed by your device's
  platform and Google's terms, not by TripToExcel.
- **Google Maps** opens only when you tap a map link. The app simply hands a URL
  to the Maps app or browser; no trip data is sent by TripToExcel.

No other third party receives data from the app.

## 7. Permissions and why they are needed

- `BLUETOOTH_CONNECT` — detect when your registered car connects or disconnects.
- `ACCESS_FINE_LOCATION` — GPS fixes for distance and addresses while recording.
- `ACCESS_BACKGROUND_LOCATION` (“Allow all the time”) — so monitoring can resume
  by itself after a phone reboot. Android does not allow a location service to
  start in the background without it.
- `POST_NOTIFICATIONS` — show the persistent “Monitoring active” notification.
- `RECEIVE_BOOT_COMPLETED` — restart monitoring after the phone reboots.

Monitoring is never silent: while it is active, the notification is always
visible, and dismissing that notification stops monitoring.

## 8. Children

TripToExcel is not directed at children and does not knowingly collect any
information from children.

## 9. Changes to this policy

If the app's behavior changes in a way that affects this policy (for example, if a
future version adds syncing), this page will be updated before that version is
released, and the “Last updated” date above will change.

## 10. Contact

Questions about this policy or about the app's data handling:
[anthony.jourdan@gmail.com](mailto:anthony.jourdan@gmail.com).

---

TripToExcel — privacy policy. This page is a static file: it sets no cookies and
runs no scripts. Source code:
[github.com/terman37/TripLogger](https://github.com/terman37/TripLogger).
