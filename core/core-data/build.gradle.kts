import org.gradle.api.tasks.testing.Test

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
}

tasks.withType<Test>().configureEach {
    onlyIf {
        project.file("src/test").exists() || project.file("src/androidTest").exists()
    }
}
