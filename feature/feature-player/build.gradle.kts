plugins {
    id("radiowave.android.feature")
}

android {
    namespace = "de.darksoon.radiowave.feature.player"
}

// core-data, core-player and core-ui come transitively from the feature
// convention plugin — only declare additional/non-convention deps here.
dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-cast"))
    // REA 95fcb33: "Titel merken"/"Gemerkte Titel" im Player (Modul vom Agenten "startseite").
    implementation(project(":feature:feature-savedtracks"))

    implementation(libs.media3.cast)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
