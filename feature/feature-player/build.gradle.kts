plugins {
    id("radiowave.android.feature")
}

android {
    namespace = "de.darksoon.radiowave.feature.player"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-cast"))

    implementation(libs.media3.cast)
}
