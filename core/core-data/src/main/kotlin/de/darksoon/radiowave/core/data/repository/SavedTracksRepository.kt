// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.darksoon.radiowave.core.model.SavedTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/*
 * REA 95fcb33: Repository = p000/u94.java, DataStore-Property = p000/v94.java
 * ("saved_tracks"), Key "bookmarks_v1", Speichern = p000/wo0.java (default-Zweig) +
 * p000/d00.java Fall 6, Entfernen = d00.java Fall 5 (über C0485m8 Fall 9),
 * Alles löschen = p000/C0475lz.java Fall 13 (über rn0 Fall 2).
 * Hilt-Provider liegt im selben Singleton-Switch wie SettingsRepository (p000/C0498ml.java).
 * REA-UNSICHER: Klassen-/Methodennamen und das Paket core.data.repository sind geraten.
 */

// Separate file from "settings" so clearing settings never touches the bookmarks.
private val Context.savedTracksStore by preferencesDataStore(name = "saved_tracks")

/**
 * Local song bookmarks ("Gemerkte Titel"): at most [MAX_TRACKS] entries, newest first,
 * stored as one JSON array in a Preferences DataStore. Nothing leaves the device.
 */
@Singleton
class SavedTracksRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = stringPreferencesKey("bookmarks_v1")
    private val json = Json { ignoreUnknownKeys = true }

    private val serializer = ListSerializer(SavedTrack.serializer())

    /** Newest first. Throws (through the flow) when the stored JSON can't be decoded. */
    val tracks: Flow<List<SavedTrack>> = context.savedTracksStore.data.map { prefs -> decode(prefs[key]) }

    /**
     * Bookmarks the given ICY title/artist. A blank title is ignored. Saving a song that is
     * already bookmarked moves it to the top (with the new station and timestamp).
     */
    suspend fun save(title: String, artist: String?, stationName: String) {
        val now = System.currentTimeMillis()
        val track = buildTrack(title, artist, stationName, now) ?: return
        context.savedTracksStore.edit { prefs ->
            val existing = decode(prefs[key])
            val updated = (listOf(track) + existing.filterNot { it.id == track.id }).take(MAX_TRACKS)
            prefs[key] = json.encodeToString(serializer, updated)
        }
    }

    suspend fun remove(id: String) {
        context.savedTracksStore.edit { prefs ->
            val remaining = decode(prefs[key]).filterNot { it.id == id }
            prefs[key] = json.encodeToString(serializer, remaining)
        }
    }

    suspend fun clear() {
        context.savedTracksStore.edit { prefs -> prefs.remove(key) }
    }

    private fun decode(raw: String?): List<SavedTrack> =
        raw?.let { json.decodeFromString(serializer, it) } ?: emptyList()

    companion object {
        const val MAX_TRACKS = 100
        private const val MAX_TITLE_LENGTH = 240
        private const val MAX_TEXT_LENGTH = 160

        /** Null when [title] is blank after cleaning. */
        internal fun buildTrack(title: String, artist: String?, stationName: String, savedAt: Long): SavedTrack? {
            val cleanTitle = clean(title, MAX_TITLE_LENGTH).takeUnless { it.isBlank() } ?: return null
            val cleanArtist = clean(artist ?: "", MAX_TEXT_LENGTH).takeUnless { it.isBlank() }
            val idSource = clean(cleanArtist ?: "", MAX_TEXT_LENGTH).lowercase(Locale.ROOT) +
                "\u0000" + clean(cleanTitle, MAX_TITLE_LENGTH).lowercase(Locale.ROOT)
            val digest = MessageDigest.getInstance("SHA-256").digest(idSource.toByteArray(Charsets.UTF_8))
            return SavedTrack(
                id = digest.joinToString("") { "%02x".format(it) },
                title = cleanTitle,
                artist = cleanArtist,
                stationName = clean(stationName, MAX_TEXT_LENGTH),
                savedAt = savedAt,
            )
        }

        /** Drops control characters (ICY garbage), trims and caps the length. */
        private fun clean(value: String, maxLength: Int): String =
            value.filterNot { Character.isISOControl(it) }.trim().take(maxLength)
    }
}
