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
val appVersionCode = providers
    .gradleProperty("app.versionCode")
    .orNull
    ?.toIntOrNull()
    ?: 14
val appVersionName = providers
    .gradleProperty("app.versionName")
    .orNull
    ?: "1.0.0-rc1"

android {
    namespace = "de.darksoon.radiowave"

    flavorDimensions += "distribution"

    defaultConfig {
        applicationId = "de.darksoon.radiowave"
        versionCode = appVersionCode
        versionName = appVersionName
    }

    productFlavors {
        create("github") {
            dimension = "distribution"
        }
        create("play") {
            dimension = "distribution"
        }
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
            ndk {
                debugSymbolLevel = "FULL"
            }
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

    implementation(libs.media3.cast)
    implementation(libs.google.material)
    
    implementation(project(":feature:feature-home"))
    implementation(project(":feature:feature-browse"))
    implementation(project(":feature:feature-favorites"))
    implementation(project(":feature:feature-player"))
implementation(project(":feature:feature-settings"))
    
    implementation(project(":auto"))
    
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
}
