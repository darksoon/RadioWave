pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RadioWave"
include(":app")

// Core modules
include(":core:core-model")
include(":core:core-database")
include(":core:core-network")
include(":core:core-data")
include(":core:core-player")
include(":core:core-cast")
include(":core:core-ui")

// Feature modules
include(":feature:feature-home")
include(":feature:feature-browse")
include(":feature:feature-favorites")
include(":feature:feature-player")
include(":feature:feature-custom-stations")
include(":feature:feature-settings")

// Android Auto
include(":auto")
