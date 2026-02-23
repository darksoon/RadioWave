plugins {
    id("radiowave.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "de.radiowave.core.network"
}

dependencies {
    implementation(project(":core:core-model"))
    
    implementation(libs.bundles.network)
}
