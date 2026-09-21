// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ordered station UUIDs of the last successfully fetched top list, so "Top" shows
 * the real ranking offline instead of whatever was cached last (after a mirror
 * sync that would be the last imported directory page). The station rows
 * themselves stay in Room; only membership and order live here — one UUID per
 * line in a small file under filesDir, which survives clearing the app cache
 * and needs no schema migration.
 */
@Singleton
class TopStationsStore internal constructor(private val file: File) {

    @Inject constructor(@ApplicationContext context: Context) : this(File(context.filesDir, FILE_NAME))

    private val mutex = Mutex()
    @Volatile private var cached: List<String>? = null

    suspend fun read(): List<String> {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: withContext(Dispatchers.IO) {
                runCatching { if (file.exists()) file.readLines() else emptyList() }
                    .getOrDefault(emptyList())
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }.also { cached = it }
        }
    }

    suspend fun write(uuids: List<String>) {
        val cleaned = uuids.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        mutex.withLock {
            if (cleaned == cached) return
            withContext(Dispatchers.IO) {
                runCatching {
                    // Write-then-rename so a crash mid-write never leaves a torn list.
                    val tmp = File(file.parentFile, "${file.name}.tmp")
                    tmp.writeText(cleaned.joinToString(separator = "\n"))
                    if (!tmp.renameTo(file)) {
                        file.delete()
                        tmp.renameTo(file)
                    }
                }
            }
            cached = cleaned
        }
    }

    private companion object {
        const val FILE_NAME = "top_stations.txt"
    }
}
