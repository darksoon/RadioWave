// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

object AppShortcuts {
    private const val TAG = "AppShortcuts"

    fun sync(context: Context) {
        val shortcuts = listOf(
            shortcut(
                context = context,
                id = "open_search",
                shortLabel = context.getString(R.string.shortcut_search_short),
                longLabel = context.getString(R.string.shortcut_search_long),
                action = ACTION_OPEN_SEARCH,
            ),
            shortcut(
                context = context,
                id = "open_favorites",
                shortLabel = context.getString(R.string.shortcut_favorites_short),
                longLabel = context.getString(R.string.shortcut_favorites_long),
                action = ACTION_OPEN_FAVORITES,
            ),
            shortcut(
                context = context,
                id = "open_player",
                shortLabel = context.getString(R.string.shortcut_player_short),
                longLabel = context.getString(R.string.shortcut_player_long),
                action = ACTION_OPEN_PLAYER,
            ),
            shortcut(
                context = context,
                id = "open_settings",
                shortLabel = context.getString(R.string.shortcut_settings_short),
                longLabel = context.getString(R.string.shortcut_settings_long),
                action = ACTION_OPEN_SETTINGS,
            ),
        )

        runCatching {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            ShortcutManagerCompat.addDynamicShortcuts(context, shortcuts)
        }.onFailure { error ->
            Log.w(TAG, "Skipping dynamic shortcut sync", error)
        }
    }

    private fun shortcut(
        context: Context,
        id: String,
        shortLabel: String,
        longLabel: String,
        action: String,
    ): ShortcutInfoCompat {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(shortLabel)
            .setLongLabel(longLabel)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()
    }
}

