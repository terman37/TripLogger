plugins {
    alias(libs.plugins.android.application)
    // Compose compiler plugin: required since Kotlin 2.0, turns @Composable
    // functions into Compose runtime calls. Version matches built-in Kotlin.
    alias(libs.plugins.compose.compiler)
    // KSP (Kotlin Symbol Processing): generates Room DAO implementations at
    // compile time from the @Dao annotations.
    alias(libs.plugins.ksp)
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
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
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