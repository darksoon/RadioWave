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

    implementation(libs.media3.cast)
}
