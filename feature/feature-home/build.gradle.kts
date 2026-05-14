plugins {
    id("radiowave.android.feature")
}

android {
    namespace = "de.darksoon.radiowave.feature.home"
}

// core-data, core-player and core-ui come transitively from the feature
// convention plugin — only declare additional/non-convention deps here.
dependencies {
    implementation(project(":core:core-model"))
}
