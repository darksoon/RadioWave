plugins {
    id("radiowave.android.feature")
}

android {
    namespace = "de.darksoon.radiowave.feature.settings"
}

// core-data, core-player and core-ui come transitively from the feature
// convention plugin — only declare additional/non-convention deps here.
dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-database"))
    implementation(libs.androidx.appcompat)
    // REA 95fcb33: für AudioOutputButton (Eintrag „Audioausgabe“, Kategorie Auto & Geräte).
    // REA-UNSICHER: Im APK nicht unterscheidbar, ob die Komponente in feature-player oder
    // core-ui liegt; ihre Strings (audio_*) wurden dem Player-Modul zugeordnet. Liegt sie
    // in core-ui, diese Zeile entfernen (core-ui kommt über die Convention).
    implementation(project(":feature:feature-player"))
}
