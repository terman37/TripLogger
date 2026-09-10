# Building Trip Logger

How to produce an installable APK. For development use the debug build; a
signed release build is optional (personal use).

## Prerequisites

- JDK 17 (the Gradle toolchain resolves/builds with it).
- Android SDK with platform 37 + build-tools (Android Studio installs these).
- `adb` (Android SDK `platform-tools`) on PATH, or use the full path
  `~/Android/Sdk/platform-tools/adb`.
- A phone with USB debugging or wireless debugging enabled
  (Developer options → Wireless debugging → pair). For wireless:
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

## Optional: signed release build

A release APK is smaller/faster and can be shared, but needs a signing key you
keep forever (losing it means future updates cannot install over the old app).

1. Create a keystore (once, keep the file + passwords safe):

```bash
keytool -genkeypair -v -keystore triplogger-release.jks \
  -alias triplogger -keyalg RSA -keysize 2048 -validity 10000
```

2. Tell Gradle about it locally (do NOT commit secrets). Either set environment
   variables and add a `signingConfig` in `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("TRIPLOGGER_KEYSTORE") ?: "triplogger-release.jks")
            storePassword = System.getenv("TRIPLOGGER_STORE_PASSWORD")
            keyAlias = "triplogger"
            keyPassword = System.getenv("TRIPLOGGER_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

3. Build + install:

```bash
./gradlew :app:assembleRelease      # app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Versioning

`versionCode` / `versionName` live in `app/build.gradle.kts` (`defaultConfig`).
Increase `versionCode` for every release APK you install over an older one.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| `No connected devices!` | `adb devices` empty → reconnect USB / `adb connect <IP:PORT>` |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Different signing key or downgrade: uninstall the app first (this deletes its database) |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | Increase `versionCode` or uninstall |
| Foreground service stops when battery saver is strict | Exempt Trip Logger from battery optimization in Android settings (monitoring is a foreground service; some OEMs still restrict) |
| Monitoring stops after driving | Check the notification is still there; if it was swiped away, monitoring is off by design (turn the Home switch back on) |
