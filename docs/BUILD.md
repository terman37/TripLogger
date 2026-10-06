# Building KmExpense

How to produce an installable APK or the signed bundle for Google Play.

## Prerequisites

- JDK 17 (the Gradle toolchain resolves/builds with it).
- Android SDK with platform 37 + build-tools (Android Studio installs these).
  The SDK path is machine-specific and therefore **not** in the repository
  (`local.properties` is gitignored), so a fresh clone needs one of:
  Android Studio (which writes `local.properties` for you), or an
  `ANDROID_HOME` environment variable pointing at the SDK. Without either, even
  `assembleDebug` stops with "SDK location not found". Example:
  `export ANDROID_HOME=$HOME/Android/Sdk`.
- `adb` (Android SDK `platform-tools`) on PATH, or use the full path
  `~/Android/Sdk/platform-tools/adb`.
- A phone with USB debugging or wireless debugging enabled  (Developer options → Wireless debugging → pair). For wireless:
  `adb pair <IP:PORT>` (code from the dialog), then `adb connect <IP:PORT>`.
  `adb devices` must list the phone.

## Debug build (recommended)

```bash
./gradlew :app:assembleDebug
```

Result: `app/build/outputs/apk/debug/app-debug.apk`

Install directly to the connected device (builds if needed):

```bash
./gradlew :app:installDebug
```

Or install a built APK manually:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Notes:

- Debug builds are perfectly fine for daily personal use. They are signed with
  the Android debug key (stable on this machine) so updates install over each
  other without uninstalling.
- They include verbose logging and are not optimized — slower startup, slightly
  larger APK — but no expiry and no limitations.
- The package is `com.terman37.triplogger`, minSdk 34 (Android 14+).

## Tests

```bash
./gradlew :app:testDebugUnitTest           # JVM unit tests (fast)
./gradlew :app:connectedDebugAndroidTest   # Room DAO tests on a device
```

## Signed release build

Debug builds are fine for personal use; the published build on Google Play must
be a **signed AAB**. The upload key is what Play uses to accept updates: keep the
`.jks` file and its passwords forever (Play App Signing can reset a lost *upload*
key, but not the app signing key).

`app/build.gradle.kts` already contains the signing configuration. It reads the
credentials from `keystore.properties` (repository root) or, if that file is
absent, from `TRIPLOGGER_*` environment variables. With neither present the
release variant is left unsigned, so debug builds and tests still work.

1. You already have an upload key from Android Studio (`androidstudio.jks`,
alias `TRIPLOGG` — confirm with
`keytool -list -v -keystore <file>`). If you ever need a new one:

```bash
keytool -genkeypair -v -keystore upload-key.jks \
  -alias triplogger -keyalg RSA -keysize 2048 -validity 10000
```

2. Copy `keystore.properties.example` to `keystore.properties` and fill it in
   (`storeFile` may be absolute or relative to the repository root).
   `storeFile`, `storePassword` and `keyAlias` are required; `keyPassword` is
   optional and defaults to `storePassword`, which is correct for PKCS12 stores
   (the `.jks` files Android Studio exports are PKCS12). Do not wrap values in
   quotes: `java.util.Properties` keeps the quotes as part of the password.

```properties
storeFile=/path/to/upload-key.jks
storePassword=...
keyAlias=triplogger
keyPassword=...
```

3. Build the bundle for Play:

```bash
./gradlew :app:bundleRelease     # app/build/outputs/bundle/release/app-release.aab
```

R8 shrinking is enabled for release, so keep `app/build/outputs/mapping/release/`
from the release you publish — it is what turns a crash stack trace back into
readable names.

For a plain installable release APK instead of a bundle:

```bash
./gradlew :app:assembleRelease   # app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Versioning

`versionCode` / `versionName` live in `app/build.gradle.kts` (`defaultConfig`).
Increase `versionCode` for every release you upload; Play rejects a bundle whose
`versionCode` was already used.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| `No connected devices!` | `adb devices` empty → reconnect USB / `adb connect <IP:PORT>` |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Different signing key or downgrade: uninstall the app first (this deletes its database) |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | Increase `versionCode` or uninstall |
| Foreground service stops when battery saver is strict | Exempt KmExpense from battery optimization in Android settings (monitoring is a foreground service; some OEMs still restrict) |
| Monitoring stops after driving | Check the notification is still there; if it was swiped away, monitoring is off by design (turn the Home switch back on) |
