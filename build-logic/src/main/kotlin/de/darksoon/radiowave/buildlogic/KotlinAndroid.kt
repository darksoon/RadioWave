// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *,>,
) {
    // Direct property assignments here (instead of nested DSL blocks like
    // `defaultConfig { ... }`) — AGP 9 changed those block signatures from
    // Function1 to Action, and the Kotlin SAM conversion at the build-logic
    // call site emits bytecode that NoSuchMethodErrors against the new API.
    commonExtension.compileSdk = 36
    commonExtension.defaultConfig.minSdk = 26
    commonExtension.defaultConfig.testInstrumentationRunner =
        "androidx.test.runner.AndroidJUnitRunner"
    commonExtension.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    commonExtension.compileOptions.targetCompatibility = JavaVersion.VERSION_17
    commonExtension.testOptions.unitTests.isIncludeAndroidResources = true
    commonExtension.packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"

    configureKotlin()
}

private fun Project.configureKotlin() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                listOf(
                    "-opt-in=kotlin.RequiresOptIn",
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-opt-in=kotlinx.coroutines.FlowPreview",
                ),
            )
        }
    }
}

