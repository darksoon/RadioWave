plugins {
    id("radiowave.android.feature")
}

android {
    namespace = "de.radiowave.feature.settings"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-database"))
    implementation(libs.androidx.appcompat)
}
