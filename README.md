# Trip Logger

An Android app that **logs your car trips automatically** and lets you export
them as a spreadsheet for your expense report.

You do not need to press anything before driving: when your car's Bluetooth
connects, the trip starts; when it disconnects, the trip ends. Distance,
start/end addresses and times are recorded. Later you pick a date range and
export a spreadsheet you can send to your employer or accountant.

Everything stays **on your phone**. There is no account, no server and nothing
is uploaded by the app.

---

## Installing the app

The app is installed manually (it is not published on the Play Store). The
build/install steps are in [BUILD.md](BUILD.md); if someone already gave you an
`.apk` file, just open it on your phone and allow installing apps from that
source.

Requirement: Android 14 or newer.

---

## First-time setup (2 minutes)

1. **Pair your car in Android settings** (the app never does the pairing
   itself): Settings → Bluetooth → pair your car/head unit or your usual car
   adapter.
2. Open **Trip Logger** and go to the **Devices** tab.
3. Tap **Allow Bluetooth access**, then tap **+** next to your car in the
   *Available* list. Your car now appears under *Registered*.
4. Go back to **Home** (first tab) and turn on the **Monitor trips** switch.
   Android asks for permissions (Bluetooth, Location, Notifications): accept
   all three — they are required.
5. A small "Monitoring active" notification appears. Setup is done.

You can log several cars/devices: add each one with **+**. Remove one with the
**X** next to it.

### The reconnect grace period (Devices tab)

Bluetooth sometimes drops for a few seconds (phone in a pocket, tunnel…). The
**Reconnect grace period** (1–15 minutes, default 3) means a disconnect only
ends the trip after that delay: if the car reconnects in time, the trip
continues as a single trip.

---

## Daily use

**Driving**

Just drive. When the car connects, the Home screen switches to a colored
"Recording" card showing the live distance and elapsed time. When the car
disconnects, the trip is saved after the grace period.

**If automatic detection misses a trip** (rare), tap **Start manually** on
Home; tap **Stop** when you arrive. Manual trips ignore Bluetooth entirely.

**Trip list (Home)** — shows today's and yesterday's trips, newest first.

- Tap a trip to see details: times, duration, addresses, distance.
- The little map icon on the row opens the whole route in Google Maps; in the
  details, each address has its own pin icon that opens just that place.
- The red trash icon removes a trip (always asks for confirmation).

A trip shorter than 50 meters is ignored — for example when the engine runs
while the car stays parked and Bluetooth connects.

---

## Reports and spreadsheets

Open the **Report** tab:

1. Pick a **From** and **To** date (side by side). By default the last 7 days
   are shown; the list updates as soon as you change a date.
2. A summary line shows how many trips and total kilometers are in the range.
3. Tap the **export button** (bottom right) to create the spreadsheet: Android's
   share sheet opens, so you can send it by mail, save it to Drive, or open it
   in Google Sheets/Excel.
4. The **trash icon** (bottom left) deletes **all** trips in the shown period —
   it always asks for confirmation first.

The spreadsheet contains one row per trip: start and end date/time as real Excel dates (Excel
shows them in your language; use Format Cells to change), the start and end address (each opens the place in Google
Maps), the kilometers, and a "trip" link opening the whole route. The header
row is frozen and light gray and the total row is highlighted. Every trip row
can also be deleted from the report with its red trash icon.

> If a trip ended while you had no network, its addresses appear as
> "Address pending". They fill in by themselves the next time you open the
> Report tab or export, as soon as you have network again.

---

## Good to know

- **The notification is monitoring.** If you swipe the "Trip Logger"
  notification away, the app stops monitoring (that is intentional: it never
  tracks silently). Turn the switch on Home back on if that was a mistake.
- **Closing the app is fine.** Monitoring continues in the background while the
  switch is on.
- **Force-stopping the app** (or rebooting the phone) during a trip loses that
  in-progress trip; the app never saves half trips.
- **Battery**: the app asks for GPS only while a trip is recording (one fix
  every 30 seconds), so it is light on battery. Some phone brands additionally
  restrict background apps; if recording stops unexpectedly, allow Trip Logger
  to run in the background / disable battery optimization for it.
- **Permissions**: Bluetooth is used only to notice your car connecting;
  location is used for distance and addresses. Nothing leaves the phone.

---

## More info

- **[DETAILS.md](DETAILS.md)** — technical documentation: architecture, how a
  trip is recorded, decisions, package layout, tests (for the curious or
  developers).
- **[BUILD.md](BUILD.md)** — how to build and install the app yourself.
- **[UI.md](UI.md)** — detailed interface specification.
- **[todo.md](todo.md)** — planned features (e.g. ticket photos).
