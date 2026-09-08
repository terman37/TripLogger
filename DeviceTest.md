# Trip Logger — device validation checklist

Run on a real phone (or emulator where noted). Goal: prove the whole flow
works before release polish. Do NOT skip steps: each catches a different
failure mode.

## 0. Setup

1. Phone with Android 14+ (minSdk 34), USB debugging enabled, connected via
   adb.
2. Build + install the debug app:

   ```bash
   ./gradlew :app:installDebug
   ```

3. For the Bluetooth tests you need at least one real paired device (a second
   phone works — the app treats ANY registered device as "the car").

## A. Automated instrumented tests (Room DAO)

Runs the database tests on the device:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected: all tests pass (trip insert/query range/update/delete round-trips).

## B. Manual scenarios

For every scenario: report what you saw vs. expected. Anything unexpected is a
bug → we fix it as its own small step before moving on.

### B1. First launch + navigation

1. Open Trip Logger. OK
2. Expected: dark theme, bottom bar with Home / Devices / Report. OK
3. Home shows gray "Monitoring off" card with an "Enable monitoring" button. OK
4. Tap it → lands on the Devices tab. OK

### B2. Permissions

1. On Devices, register nothing yet. The "Monitor trips" switch must be
   disabled with the hint "Register a device below…". OK
2. Pair a Bluetooth device in Android settings first if none is paired.
   (No in-app pairing by design.) Not OK, bluetooth access needed to list devices but there should be a invite at first launch to grant needed authorizations
3. Back in the app, the paired device must appear under "Available" (the list
   refreshes when the screen resumes).
4. Tap **+** → device moves to "Registered".
5. Now toggle "Monitor trips" ON → Android permission dialog(s) appear:
   Bluetooth, Location, Notifications. Grant all.
6. Expected: switch stays ON; persistent notification "Trip Logger —
   Monitoring active" appears; Home card shows "Waiting for …".
7. Deny-flow check (optional): revoke Location in Settings → Devices shows the
   "monitoring is paused" warning; re-grant and toggle OFF/ON to recover.

### B3. Manual trip (no Bluetooth needed)

1. Home → tap "Start manually".
2. Expected: card turns colored "Recording — Started manually", km + elapsed
   ticking; notification reads "Recording — 0.0 km" and updates while driving.
3. Move ~100 m (or use emulator mock location, see note) → distance grows.
4. Tap Stop.
5. Expected: card back to "Waiting/off"; a trip row appears on Home
   (today) with date, summary, km.
6. Tap the row → expands (times, duration, addresses, Delete). Delete it →
   row disappears.

### B4. Auto trip with grace period (needs real device pair)

Setup: register your car/second phone, monitoring ON, location ON, drive the
phone in the car.

1. Start engine / switch the paired device on → app must start a trip
   WITHOUT touching the phone (notification switches to "Recording…").
2. Drive at least 1 km, stop, switch the device off.
3. Expected: notification "Disconnected — finishing trip…"; Home card shows
   the grace state.
4. Reconnect the device within 3 minutes → same trip continues (single trip
   row at the end, distance carried over).
5. Disconnect again and leave it off → after 3 minutes the trip is finished;
   exactly one new row with plausible km appears in Home.
6. Change "Reconnect grace period" to 1 minute on Devices; repeat: trip must
   now finish after ~1 minute instead of 3.

### B5. Force-stop / restart mid-trip (KISS decision)

1. Start a trip (auto or manual), then force-stop the app (Recents → swipe
   away or Settings → Force stop).
2. Reopen the app.
3. Expected: NO half-finished trip row appears (partial trips are discarded by
   design, plan.md question 4). Home returns to normal state.

### B6. Offline trip end → address retry

1. Put the phone in airplane mode.
2. Drive a short manual trip, stop it.
3. Expected: the trip row exists; addresses show "Address pending".
4. Disable airplane mode. Reopen the app (or go to Report and tap Generate).
5. Expected: the row's addresses are now filled in (lazy retry on app open /
   at report generation, todo.md decision).

### B7. Report + export

1. Report tab: From/To default to the previous calendar month — adjust to
   cover the trips you recorded.
2. Tap Generate.
3. Expected: summary "N trips · X km"; list ordered oldest first; rows expand
   to the same detail as Home.
4. Tap "Export spreadsheet" → system share sheet appears with the CSV.
5. Send it to a spreadsheet app (Sheets/Excel) or a file app and open it.
   Expected columns (todo.md): start date, start time, start month, end date,
   end time, start city, start address, end city, end address, km — plus a
   Total row. Decimal separator must be a dot; accented characters intact;
   addresses containing commas are quoted.
6. Empty range (e.g. next year): Generate → "No trips in this period", Export
   still produces a file with header + Total 0.0 (allowed).

### B8. GPS edge cases

1. GPS off (quick settings) + start manual trip → trip records with 0.0 km;
   no crash; stop works.
2. Parked with engine on (device connected, no movement) for > 1 min → trip
   still running but distance stays 0.0 (movement filter).

## Emulator notes (when no phone is available)

- Bluetooth auto-start (B4) is NOT testable on the emulator — it has no
  real paired devices. Use a physical phone for B4/B5/B8.1.
- Location: emulator → Extended controls → Location → set/play a route, or
  `adb emu geo fix <lng> <lat>` repeatedly; 30 s sampling means points need
  to be seconds apart to see km grow.
- Everything else (B1–B3, B6, B7) works on the emulator.

## Report back

- adb output of section A,
- pass/fail per B-scenario with any error text or screenshot,
- unexpected behaviors get their own fix step before Step 13.
