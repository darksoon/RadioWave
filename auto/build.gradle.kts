plugins {
    id("radiowave.android.library")
    id("radiowave.hilt")
}

android {
    namespace = "de.darksoon.radiowave.auto"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-player"))
    
    implementation(libs.bundles.media3)
    // CarConnection: authoritative OS signal for "projection ended" — closes the
    // wireless-AA gap where BT lingers after the user left the car and neither
    // onDisconnected nor audio-becoming-noisy fire in time.
    implementation(libs.androidx.car.app)
}
