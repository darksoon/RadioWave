plugins {
    id("radiowave.android.feature")
}

android {
    namespace = "de.radiowave.feature.settings"
}

dependencies {
    implementation(project(":core:core-model"))
}
