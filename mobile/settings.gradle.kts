pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Wayside"
include(":app")

// A local working copy of the infrastructure library, included as a module (not includeBuild)
// so it shares this build's AGP, Kotlin and version catalog — two AGP versions in one build is
// an error. Not committed; see .gitignore.
include(":InfrastructureLibrary")
