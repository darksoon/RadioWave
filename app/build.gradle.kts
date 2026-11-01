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
            // REA 95fcb33: im Handy-Build fehlen gegenüber 944fec5 alle unreferenzierten
            // Ressourcen komplett aus resources.arsc (u. a. 96 Layouts, 159 Drawables,
            // 678 Styles, 229 Bibliotheks-Strings, bg_nebula) und die nur über Layouts
            // gehaltenen View-Klassen (MaterialButton, TextInputLayout, MediaRouteButton …).
            // Das ist das Muster von AGP 9 "optimized resource shrinking" → Sicherheit: hoch.
            isShrinkResources = true
            ndk {
                debugSymbolLevel = "FULL"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // REA-UNSICHER: handy-95fcb33.apk ist ein Release-Build (R8 9.2.14,
                // compilation-mode release), aber mit dem Android-Debug-Zertifikat signiert
                // (CN=Android Debug, SHA-256 52:B8:01:4B:…:3F:1B). Entweder stand hier dieser
                // Fallback, oder die APK wurde nachträglich mit dem Debug-Keystore signiert.
                signingConfig = signingConfigs.getByName("debug")
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
    // REA 95fcb33: "Gemerkte Titel" (Hilt-ViewModel muss im App-Graph sichtbar sein).
    implementation(project(":feature:feature-savedtracks"))
implementation(project(":feature:feature-settings"))
    
    implementation(project(":auto"))
    
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    // Compose Material icons — the compose convention plugin includes this in
    // its bundle as `implementation`, which doesn't propagate to dependents.
    // App itself uses Icons.* directly, so we add it explicitly here.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material.icons.extended)
}
