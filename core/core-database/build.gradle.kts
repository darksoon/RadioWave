plugins {
    id("radiowave.android.library")
    id("radiowave.hilt")
    id("radiowave.android.room")
}

android {
    namespace = "de.radiowave.core.database"
}

dependencies {
    implementation(project(":core:core-model"))
    
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
}
