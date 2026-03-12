plugins {
    id("radiowave.android.library")
    id("radiowave.hilt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "de.darksoon.radiowave.core.network"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:core-model"))
    
    implementation(libs.bundles.network)
    implementation(libs.hilt.android)
}
