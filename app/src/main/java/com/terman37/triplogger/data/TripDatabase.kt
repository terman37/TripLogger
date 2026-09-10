package com.terman37.triplogger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The app's local database (decision: local-only, no server — todo.md).
 *
 * What is a "database" here: Room is a layer over SQLite that turns Kotlin
 * classes into tables and interface functions into SQL. This class only
 * describes the schema and exposes DAOs.
 */
@Database(
    entities = [Trip::class],
    version = 1,
    // Schema export files are used to write migration tests later, when the
    // schema changes (v2+). v1 has no migrations (initial decision).
    exportSchema = false,
)
@TypeConverters(TripOriginConverter::class)
abstract class TripDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao

    companion object {
        /**
         * Singleton holder so the database is opened only once per process.
         * getName: Android keeps databases in app-private storage, no storage
         * permission needed (README).
         */
        @Volatile
        private var instance: TripDatabase? = null

        fun getInstance(context: Context): TripDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TripDatabase::class.java,
                    "triplogger.db",
                ).build().also { instance = it }
            }
    }
}
