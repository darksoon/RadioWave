plugins {
    id("radiowave.android.feature")
}

android {
    namespace = "de.radiowave.feature.favorites"
}

dependencies {
    implementation(project(":core:core-model"))
}
