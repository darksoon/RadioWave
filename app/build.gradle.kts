import java.util.Properties

plugins {
    id("radiowave.android.application")
    id("radiowave.hilt")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystorePropertiesFile.exists()

android {
    namespace = "de.radiowave"

    defaultConfig {
        applicationId = "de.radiowave"
        versionCode = 7
        versionName = "0.1.0-beta.1"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
}
