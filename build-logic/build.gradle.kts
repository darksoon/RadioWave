plugins {
    `kotlin-dsl`
}

group = "de.radiowave.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "radiowave.android.application"
            implementationClass = "de.radiowave.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "radiowave.android.library"
            implementationClass = "de.radiowave.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "radiowave.android.library.compose"
            implementationClass = "de.radiowave.buildlogic.AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "radiowave.android.feature"
            implementationClass = "de.radiowave.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("androidRoom") {
            id = "radiowave.android.room"
            implementationClass = "de.radiowave.buildlogic.AndroidRoomConventionPlugin"
        }
        register("hilt") {
            id = "radiowave.hilt"
            implementationClass = "de.radiowave.buildlogic.HiltConventionPlugin"
        }
    }
}
