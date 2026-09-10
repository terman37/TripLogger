package com.terman37.triplogger

import android.app.Application

/**
 * Application subclass (registered in the AndroidManifest). Android creates it
 * when the process starts, before any Activity or Service; it hosts the
 * [AppContainer] so every component shares the same singletons.
 */
class TripLoggerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
