plugins {
    id("radiowave.android.application")
    id("radiowave.hilt")
}

android {
    namespace = "de.radiowave"

    defaultConfig {
        applicationId = "de.radiowave"
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-player"))
    implementation(project(":core:core-cast"))
    implementation(project(":core:core-ui"))
    
    implementation(project(":feature:feature-home"))
    implementation(project(":feature:feature-browse"))
    implementation(project(":feature:feature-favorites"))
    implementation(project(":feature:feature-player"))
    implementation(project(":feature:feature-custom-stations"))
    implementation(project(":feature:feature-settings"))
    
    implementation(project(":auto"))
    
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
}
