// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URLEncoder
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

data class LocalIssueReport(
    val title: String,
    val summary: String,
    val body: String,
    val timestampMs: Long,
    val sourceFile: File,
)

object LocalIssueReporter {
    private const val REPORT_DIR = "issue-reports"
    private const val REPORT_FILE = "last_crash_report.txt"
    private const val SHARE_FILE = "radiowave-crash-report.txt"
    private const val ISSUE_URL = "https://github.com/darksoon/RadioWave/issues/new"
    private val installed = AtomicBoolean(false)

    fun install(context: Context) {
        if (!installed.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                writeCrashReport(
                    context = appContext,
                    throwable = throwable,
                    threadName = thread.name,
                )
            }
            previousHandler?.uncaughtException(thread, throwable)
                ?: throw throwable
        }
    }

    fun getLatestReport(context: Context): LocalIssueReport? {
        val sourceFile = File(getReportDirectory(context), REPORT_FILE)
        if (!sourceFile.exists()) return null
        val body = sourceFile.readText()
        val timestampMs = sourceFile.lastModified()
        val title = buildIssueTitle(timestampMs)
        return LocalIssueReport(
            title = title,
            summary = buildIssueSummary(context, timestampMs),
            body = body,
            timestampMs = timestampMs,
            sourceFile = sourceFile,
        )
    }

    fun buildIssueUrl(context: Context): String? {
        val report = getLatestReport(context) ?: return null
        val title = urlEncode(report.title)
        val body = urlEncode(buildIssueBody(report))
        return "$ISSUE_URL?title=$title&body=$body"
    }

    fun buildShareIntent(context: Context): Intent? {
        val report = getLatestReport(context) ?: return null
        val cacheFile = File(context.cacheDir, SHARE_FILE).apply {
            parentFile?.mkdirs()
            writeText(report.body)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, report.title)
            putExtra(Intent.EXTRA_TEXT, buildIssueBody(report))
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun writeCrashReport(
        context: Context,
        throwable: Throwable,
        threadName: String,
    ) {
        val reportFile = File(getReportDirectory(context), REPORT_FILE)
        reportFile.writeText(buildCrashBody(context, throwable, threadName))
    }

    private fun buildCrashBody(
        context: Context,
        throwable: Throwable,
        threadName: String,
    ): String {
        val timestampMs = System.currentTimeMillis()
        val formattedTime = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
            .format(Date(timestampMs))
        val stacktrace = StringWriter().also { writer ->
            throwable.printStackTrace(PrintWriter(writer))
        }.toString().trim()
        return buildString {
            appendLine("RadioWave Crash Report")
            appendLine("Generated: $formattedTime")
            appendLine("App version: ${readVersionName(context)} (${readVersionCode(context)})")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Brand/Product: ${Build.BRAND} / ${Build.PRODUCT}")
            appendLine("Thread: $threadName")
            appendLine()
            appendLine("Exception:")
            appendLine(throwable.javaClass.name)
            appendLine(throwable.message ?: "No exception message")
            appendLine()
            appendLine("Stacktrace:")
            appendLine(stacktrace)
        }.trim()
    }

    private fun buildIssueBody(report: LocalIssueReport): String {
        val excerpt = report.body
            .lineSequence()
            .take(16)
            .joinToString("\n")
        return buildString {
            appendLine("## Crash report")
            appendLine()
            appendLine("_Please review and remove anything you do not want to share before posting._")
            appendLine()
            appendLine("Summary: ${report.summary}")
            appendLine()
            appendLine("### Report excerpt")
            appendLine("```")
            appendLine(excerpt)
            appendLine("```")
            appendLine()
            appendLine("### Full report")
            appendLine("Attach the shared crash-report file from RadioWave if possible.")
        }.trim()
    }

    private fun buildIssueSummary(context: Context, timestampMs: Long): String {
        val formattedTime = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(timestampMs))
        return "Crash on ${Build.MANUFACTURER} ${Build.MODEL} running Android ${Build.VERSION.RELEASE} at $formattedTime"
    }

    private fun buildIssueTitle(timestampMs: Long): String {
        val formattedDate = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(timestampMs))
        return "Crash report: $formattedDate"
    }

    private fun getReportDirectory(context: Context): File {
        return File(context.filesDir, REPORT_DIR).apply { mkdirs() }
    }

    private fun readVersionName(context: Context): String {
        return runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "unknown"
        }.getOrDefault("unknown")
    }

    private fun readVersionCode(context: Context): Long {
        return runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        }.getOrDefault(-1L)
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
            .replace("+", "%20")
    }
}

