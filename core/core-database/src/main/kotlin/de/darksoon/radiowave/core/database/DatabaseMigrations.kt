// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.database

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

    /**
     * Repairs rows created by [MIGRATION_1_2]/[MIGRATION_2_3]: those migrations added
     * `stationName`/`streamUrl` with `DEFAULT ''` instead of backfilling from the existing
     * `stations` cache table, so any favorite/recent that predated the v1->v2 / v2->v3 bump
     * silently lost its name and playable URL (renders as a blank card, plays nothing).
     *
     * This migration re-attempts the backfill from `stations` (best-effort — the cache row
     * may have expired since) and, for rows that still can't be repaired, removes them rather
     * than leaving permanently blank/unplayable ghost entries in Favorites/Recents.
     */
    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                UPDATE favorites
                SET stationName = (SELECT stations.name FROM stations WHERE stations.uuid = favorites.stationUuid),
                    streamUrl = (SELECT stations.streamUrl FROM stations WHERE stations.uuid = favorites.stationUuid)
                WHERE stationName = ''
                  AND EXISTS (
                      SELECT 1 FROM stations
                      WHERE stations.uuid = favorites.stationUuid AND stations.streamUrl != ''
                  )
                """.trimIndent(),
            )
            database.execSQL("DELETE FROM favorites WHERE stationName = '' OR streamUrl = ''")

            database.execSQL(
                """
                UPDATE recent_stations
                SET stationName = (SELECT stations.name FROM stations WHERE stations.uuid = recent_stations.stationUuid),
                    streamUrl = (SELECT stations.streamUrl FROM stations WHERE stations.uuid = recent_stations.stationUuid)
                WHERE stationName = ''
                  AND EXISTS (
                      SELECT 1 FROM stations
                      WHERE stations.uuid = recent_stations.stationUuid AND stations.streamUrl != ''
                  )
                """.trimIndent(),
            )
            database.execSQL("DELETE FROM recent_stations WHERE stationName = '' OR streamUrl = ''")
        }
    }
}

