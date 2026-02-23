plugins {
    id("radiowave.android.library.compose")
    id("radiowave.hilt")
}

android {
    namespace = "de.radiowave.core.ui"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-player"))
    
    api(libs.androidx.material3)
    api(libs.coil.compose)
}
