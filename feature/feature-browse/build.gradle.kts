plugins {
    id("radiowave.android.feature")
}

android {
    namespace = "de.radiowave.feature.browse"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-ui"))
    implementation(project(":feature:feature-home"))
}
