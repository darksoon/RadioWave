plugins {
    id("radiowave.android.feature")
}

android {
    namespace = "de.darksoon.radiowave.feature.favorites"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-ui"))
}
