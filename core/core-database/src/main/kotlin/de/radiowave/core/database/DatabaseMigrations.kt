// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                ALTER TABLE recent_stations ADD COLUMN stationName TEXT NOT NULL DEFAULT ''
                """.trimIndent(),
            )
            database.execSQL(
                """
                ALTER TABLE recent_stations ADD COLUMN streamUrl TEXT NOT NULL DEFAULT ''
                """.trimIndent(),
            )
            database.execSQL("ALTER TABLE recent_stations ADD COLUMN homepageUrl TEXT")
            database.execSQL("ALTER TABLE recent_stations ADD COLUMN faviconUrl TEXT")
            database.execSQL("ALTER TABLE recent_stations ADD COLUMN country TEXT")
            database.execSQL("ALTER TABLE recent_stations ADD COLUMN countryCode TEXT")
            database.execSQL("ALTER TABLE recent_stations ADD COLUMN language TEXT")
            database.execSQL("ALTER TABLE recent_stations ADD COLUMN tags TEXT")
            database.execSQL("ALTER TABLE recent_stations ADD COLUMN codec TEXT")
            database.execSQL("ALTER TABLE recent_stations ADD COLUMN bitrate INTEGER")
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                ALTER TABLE favorites ADD COLUMN stationName TEXT NOT NULL DEFAULT ''
                """.trimIndent(),
            )
            database.execSQL(
                """
                ALTER TABLE favorites ADD COLUMN streamUrl TEXT NOT NULL DEFAULT ''
                """.trimIndent(),
            )
            database.execSQL("ALTER TABLE favorites ADD COLUMN homepageUrl TEXT")
            database.execSQL("ALTER TABLE favorites ADD COLUMN faviconUrl TEXT")
            database.execSQL("ALTER TABLE favorites ADD COLUMN country TEXT")
            database.execSQL("ALTER TABLE favorites ADD COLUMN countryCode TEXT")
            database.execSQL("ALTER TABLE favorites ADD COLUMN language TEXT")
            database.execSQL("ALTER TABLE favorites ADD COLUMN tags TEXT")
            database.execSQL("ALTER TABLE favorites ADD COLUMN codec TEXT")
            database.execSQL("ALTER TABLE favorites ADD COLUMN bitrate INTEGER")
        }
    }
}

