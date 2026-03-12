plugins {
    id("radiowave.android.library")
    id("radiowave.hilt")
}

android {
    namespace = "de.darksoon.radiowave.core.cast"
}

dependencies {
    implementation(project(":core:core-player"))
    implementation(project(":core:core-model"))
    
    implementation(libs.media3.cast)
}
