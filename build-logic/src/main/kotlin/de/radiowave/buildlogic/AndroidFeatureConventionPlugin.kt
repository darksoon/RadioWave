// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("radiowave.android.library.compose")
            pluginManager.apply("radiowave.hilt")

            dependencies {
                add("implementation", project(":core:core-ui"))
                add("implementation", project(":core:core-data"))
                add("implementation", project(":core:core-player"))

                add("implementation", libs.findLibrary("androidx-navigation-compose").get())
                add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libs.findLibrary("coil-compose").get())
            }
        }
    }
}

