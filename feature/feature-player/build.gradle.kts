plugins {
    id("radiowave.android.feature")
}

android {
    namespace = "de.radiowave.feature.player"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-data"))
}
