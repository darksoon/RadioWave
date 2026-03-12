plugins {
    id("radiowave.android.library")
    id("radiowave.hilt")
}

android {
    namespace = "de.darksoon.radiowave.auto"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-player"))
    
    implementation(libs.bundles.media3)
}
