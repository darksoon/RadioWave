plugins {
    id("radiowave.android.library")
    id("radiowave.hilt")
}

android {
    namespace = "de.radiowave.core.cast"
}

dependencies {
    implementation(project(":core:core-player"))
    implementation(project(":core:core-model"))
    
    implementation(libs.media3.cast)
}
