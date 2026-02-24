import org.gradle.api.tasks.testing.Test

plugins {
    id("radiowave.android.library")
    id("radiowave.hilt")
}

android {
    namespace = "de.radiowave.auto"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-player"))
    
    implementation(libs.bundles.media3)
}

tasks.withType<Test>().configureEach {
    onlyIf {
        project.file("src/test").exists() || project.file("src/androidTest").exists()
    }
}
