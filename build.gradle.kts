// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // ./gradlew dependencyUpdates  →  reports/dependencyUpdates/report.txt
    alias(libs.plugins.versions)
}

// Filter out alphas/betas/RCs from the dependencyUpdates report so it only
// surfaces real stable upgrades — Compose BOM and Media3 ship a lot of
// pre-releases that we don't want flagged.
tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates") {
    val unstableMarkers = listOf("alpha", "beta", "rc", "dev", "m", "preview")
    rejectVersionIf {
        val lower = candidate.version.lowercase()
        unstableMarkers.any { marker -> lower.contains("-$marker") || lower.contains(".$marker") }
    }
    // Skip Gradle release-candidate notifications from the same task.
    checkForGradleUpdate = true
    outputFormatter = "plain,html"
}
