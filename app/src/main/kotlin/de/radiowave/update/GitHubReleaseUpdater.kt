package de.radiowave.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateRelease(
    val tag: String,
    val title: String,
    val body: String,
    val htmlUrl: String,
    val apkUrl: String,
    val apkName: String,
)

object GitHubReleaseUpdater {
    private const val OWNER = "darksoon"
    private const val REPO = "RadioWave"
    private const val RELEASES_URL = "https://api.github.com/repos/$OWNER/$REPO/releases?per_page=8"
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 20_000

    suspend fun checkForUpdate(currentVersionName: String): UpdateRelease? = withContext(Dispatchers.IO) {
        val currentVersion = normalizeVersion(currentVersionName)
        val json = requestJsonArray(RELEASES_URL) ?: return@withContext null
        val release = parseLatestInstallableRelease(json) ?: return@withContext null
        val candidateVersion = normalizeVersion(release.tag)
        if (candidateVersion == currentVersion) return@withContext null
        release
    }

    suspend fun downloadAndStartInstall(
        context: Context,
        release: UpdateRelease,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            ensureInstallPermission(context)
            val updatesDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "updates",
            ).apply { mkdirs() }
            val targetFile = File(updatesDir, sanitizeFileName(release.apkName))
            downloadFile(url = release.apkUrl, targetFile = targetFile)
            launchInstaller(context, targetFile)
        }
    }

    private fun parseLatestInstallableRelease(array: JSONArray): UpdateRelease? {
        for (index in 0 until array.length()) {
            val release = array.optJSONObject(index) ?: continue
            if (release.optBoolean("draft", false)) continue
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
            return UpdateRelease(
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

    private fun downloadFile(url: String, targetFile: File) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", "RadioWave-Android")
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("Download failed: HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun launchInstaller(context: Context, apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            data = apkUri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "application/vnd.android.package-archive"
        }
        try {
            context.startActivity(installIntent)
        } catch (_: ActivityNotFoundException) {
            error("No installer available on device")
        }
    }

    private fun ensureInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (context.packageManager.canRequestPackageInstalls()) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        error("Install unknown apps permission required")
    }

    private fun sanitizeFileName(name: String): String {
        val fallback = "radiowave-update.apk"
        val cleaned = name.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_')
        if (cleaned.isBlank()) return fallback
        return if (cleaned.endsWith(".apk", ignoreCase = true)) cleaned else "$cleaned.apk"
    }

    private fun normalizeVersion(raw: String): String {
        return raw.trim().removePrefix("v").lowercase()
    }
}
