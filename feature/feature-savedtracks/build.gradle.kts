plugins {
    id("radiowave.android.feature")
}

// REA-UNSICHER: Eigenes Modul ist eine Rekonstruktions-Entscheidung. Im Handy-Build werden
// SavedTracksButton/SaveTrackButton sowohl von der Startseite (Header) als auch vom Player
// benutzt; ein gemeinsames Feature-Modul vermeidet eine Abhängigkeit home <-> player.
android {
    namespace = "de.darksoon.radiowave.feature.savedtracks"
}

dependencies {
    implementation(project(":core:core-model"))
}
