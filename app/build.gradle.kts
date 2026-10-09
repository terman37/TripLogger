import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // Compose compiler plugin: required since Kotlin 2.0, turns @Composable
    // functions into Compose runtime calls. Version matches built-in Kotlin.
    alias(libs.plugins.compose.compiler)
    // KSP (Kotlin Symbol Processing): generates Room DAO implementations at
    // compile time from the @Dao annotations.
    alias(libs.plugins.ksp)
}

// --- Release signing ---------------------------------------------------------
// The published build must be signed with the upload key you keep forever.
// Credentials are read from, in order:
//   1. keystore.properties in the repository root (gitignored), or
//   2. TRIPLOGGER_* environment variables (CI, or to keep no file on disk).
// If neither is present the release variant stays unsigned: debug builds and
// tests keep working, but `bundleRelease` produces a bundle Play will reject.
// Example file: keystore.properties.example.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

/** Value from keystore.properties, falling back to the named environment variable. */
fun signingValue(key: String, envName: String): String? =
    (keystoreProperties.getProperty(key) ?: System.getenv(envName))?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("storeFile", "TRIPLOGGER_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "TRIPLOGGER_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "TRIPLOGGER_KEY_ALIAS")
// PKCS12 stores cannot hold a key password different from the store password
// (Android Studio's exported .jks files are PKCS12 despite the extension), so
// fall back to storePassword. A JKS store with a distinct key password still
// works: set keyPassword explicitly.
val releaseKeyPassword =
    signingValue("keyPassword", "TRIPLOGGER_KEY_PASSWORD") ?: releaseStorePassword

val releaseSigningValues =
    listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)

// Four values or none: a half-filled key is a mistake, so fail loudly instead
// of silently producing a differently signed build.
if (releaseSigningValues.any { it != null } && releaseSigningValues.any { it == null }) {
    throw GradleException(
        "Incomplete release signing configuration: set storeFile, storePassword " +
            "and keyAlias, either in keystore.properties or as TRIPLOGGER_* " +
            "environment variables (keyPassword is optional and defaults to " +
            "storePassword).",
    )
}

android {
    namespace = "com.terman37.triplogger"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.terman37.triplogger"
        minSdk = 34
        targetSdk = 37
        // versionCode: Play's counter — it must be higher than every upload,
        // including uploads to testing tracks. Bump it for every new AAB.
        // versionName: what users see; it only changes when the release itself
        // changes (see AGENTS.md and docs/release-notes.md).
        versionCode = 3
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // storeFile is resolved against the repository root, so a relative path
        // in keystore.properties is relative to the repo, not to app/.
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // R8 shrinks and obfuscates the published build. Room and Compose
            // ship their own keep rules, so no custom proguard file is needed
            // unless reflection is added later. The minified build must pass
            // the same tests as the debug build (see the release plan).
            optimization {
                enable = true
            }
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        // Enables Jetpack Compose for this module (AGP 9 built-in Kotlin handles
        // the Compose compiler).
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Compose UI. The BOM pins all androidx.compose.* versions so individual
    // libraries do not declare versions here.
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Room: runtime + KTX (suspend DAO functions); compiler runs via KSP.
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    // GrantPermissionRule for the service/notification instrumented tests.
    androidTestImplementation(libs.androidx.test.rules)
    // Room helper to open an in-memory database inside instrumented tests.
    androidTestImplementation(libs.room.testing)
}
