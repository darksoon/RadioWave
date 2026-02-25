plugins {
    id("radiowave.android.library")
    id("radiowave.hilt")
}

android {
    namespace = "de.radiowave.core.player"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-data"))
    
    implementation(libs.bundles.media3)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
