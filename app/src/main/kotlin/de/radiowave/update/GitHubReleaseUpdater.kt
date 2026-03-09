package de.radiowave.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import de.radiowave.core.data.update.GitHubReleaseChecker
import de.radiowave.core.data.update.GitHubReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class UpdateRelease(
    val tag: String,
    val title: String,
    val body: String,
    val htmlUrl: String,
    val apkUrl: String,
    val apkName: String,
)

data class UpdateDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val percent: Int?,
)

object GitHubReleaseUpdater {
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 20_000

    suspend fun checkForUpdate(
        currentVersionName: String,
        includePrerelease: Boolean = false,
    ): UpdateRelease? = withContext(Dispatchers.IO) {
        GitHubReleaseChecker.checkForUpdate(
            currentVersionName = currentVersionName,
            includePrerelease = includePrerelease,
        )?.toUpdateRelease()
    }

    suspend fun downloadAndStartInstall(
        context: Context,
        release: UpdateRelease,
        onProgress: (UpdateDownloadProgress) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            ensureInstallPermission(context)
            val updatesDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "updates",
            ).apply { mkdirs() }
            val targetFile = File(updatesDir, sanitizeFileName(release.apkName))
            downloadFile(
                url = release.apkUrl,
                targetFile = targetFile,
                onProgress = onProgress,
            )
            launchInstaller(context, targetFile)
        }
    }

    private suspend fun downloadFile(
        url: String,
        targetFile: File,
        onProgress: (UpdateDownloadProgress) -> Unit,
    ) {
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
            val totalBytes = connection.contentLengthLong.takeIf { it > 0L } ?: -1L
            publishProgress(
                onProgress = onProgress,
                downloadedBytes = 0L,
                totalBytes = totalBytes,
            )
            connection.inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloadedBytes = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        publishProgress(
                            onProgress = onProgress,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes,
                        )
                    }
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

    private suspend fun publishProgress(
        onProgress: (UpdateDownloadProgress) -> Unit,
        downloadedBytes: Long,
        totalBytes: Long,
    ) {
        val percent = if (totalBytes > 0L) {
            ((downloadedBytes.toDouble() / totalBytes.toDouble()) * 100.0).roundToInt()
                .coerceIn(0, 100)
        } else {
            null
        }
        withContext(Dispatchers.Main.immediate) {
            onProgress(
                UpdateDownloadProgress(
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
                    percent = percent,
                ),
            )
        }
    }

    private fun GitHubReleaseInfo.toUpdateRelease(): UpdateRelease = UpdateRelease(
        tag = tag,
        title = title,
        body = body,
        htmlUrl = htmlUrl,
        apkUrl = apkUrl,
        apkName = apkName,
    )
}
