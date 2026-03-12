// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubReleaseInfo(
    val tag: String,
    val title: String,
    val body: String,
    val htmlUrl: String,
    val apkUrl: String,
    val apkName: String,
)

object GitHubReleaseChecker {
    private const val OWNER = "darksoon"
    private const val REPO = "RadioWave"
    private const val RELEASES_URL = "https://api.github.com/repos/$OWNER/$REPO/releases?per_page=8"
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 20_000

    suspend fun checkForUpdate(
        currentVersionName: String,
        includePrerelease: Boolean = false,
    ): GitHubReleaseInfo? = withContext(Dispatchers.IO) {
        val currentVersion = normalizeVersion(currentVersionName)
        val latest = getLatestInstallableRelease(includePrerelease = includePrerelease) ?: return@withContext null
        val candidateVersion = normalizeVersion(latest.tag)
        if (candidateVersion == currentVersion) return@withContext null
        latest
    }

    suspend fun getLatestInstallableRelease(
        includePrerelease: Boolean = false,
    ): GitHubReleaseInfo? = withContext(Dispatchers.IO) {
        val json = requestJsonArray(RELEASES_URL) ?: return@withContext null
        parseLatestInstallableRelease(
            array = json,
            includePrerelease = includePrerelease,
        )
    }

    private fun parseLatestInstallableRelease(
        array: JSONArray,
        includePrerelease: Boolean,
    ): GitHubReleaseInfo? {
        for (index in 0 until array.length()) {
            val release = array.optJSONObject(index) ?: continue
            if (release.optBoolean("draft", false)) continue
            if (release.optBoolean("prerelease", false) && !includePrerelease) continue
            val assets = release.optJSONArray("assets") ?: continue
            val apkAsset = findApkAsset(assets) ?: continue

            val tag = release.optString("tag_name").trim()
            if (tag.isBlank()) continue
            val name = release.optString("name").trim().ifBlank { tag }
            val body = release.optString("body").trim()
            val htmlUrl = release.optString("html_url").trim()
            val downloadUrl = apkAsset.optString("browser_download_url").trim()
            val fileName = apkAsset.optString("name").trim()
            if (downloadUrl.isBlank() || fileName.isBlank()) continue
            return GitHubReleaseInfo(
                tag = tag,
                title = name,
                body = body,
                htmlUrl = htmlUrl,
                apkUrl = downloadUrl,
                apkName = fileName,
            )
        }
        return null
    }

    private fun findApkAsset(assets: JSONArray): JSONObject? {
        var fallback: JSONObject? = null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name").trim()
            if (!name.endsWith(".apk", ignoreCase = true)) continue
            if (name.contains("release", ignoreCase = true)) return asset
            if (fallback == null) fallback = asset
        }
        return fallback
    }

    private fun requestJsonArray(url: String): JSONArray? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "RadioWave-Android")
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONArray(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeVersion(raw: String): String {
        return raw.trim().removePrefix("v").lowercase()
    }
}

