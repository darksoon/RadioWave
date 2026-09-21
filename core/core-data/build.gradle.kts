plugins {
    id("radiowave.android.library")
    id("radiowave.hilt")
}

android {
    namespace = "de.darksoon.radiowave.core.data"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-network"))
    
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    // Reuses the same Json { ignoreUnknownKeys = true; coerceInputValues = true } singleton
    // NetworkModule provides for Retrofit — see FavoriteRepositoryImpl's favorites export/import.
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
