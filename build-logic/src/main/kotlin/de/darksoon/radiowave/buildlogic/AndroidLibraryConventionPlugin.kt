// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                // AGP 9+ has built-in Kotlin support; org.jetbrains.kotlin.android
                // no longer needs to be applied separately.
                apply("com.android.library")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                // defaultConfig.targetSdk is deprecated in library DSL in AGP 9;
                // libraries don't need a runtime targetSdk. Move to testOptions/lint
                // if specific test/lint behavior is needed.
                @Suppress("UnstableApiUsage")
                testOptions.targetSdk = 35
                @Suppress("UnstableApiUsage")
                testOptions.animationsDisabled = true
                lint.targetSdk = 35
            }

            dependencies {
                add("implementation", libs.findLibrary("androidx-core-ktx").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
            }

            val hasTestSources =
                layout.projectDirectory.dir("src/test").asFile.exists() ||
                    layout.projectDirectory.dir("src/androidTest").asFile.exists()

            tasks.withType<Test>().configureEach {
                onlyIf { hasTestSources }
            }
        }
    }
}

