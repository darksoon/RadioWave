plugins {
    id("radiowave.android.library")
    id("radiowave.hilt")
}

android {
    namespace = "de.radiowave.core.data"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-network"))
    
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
