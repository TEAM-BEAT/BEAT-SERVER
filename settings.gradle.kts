import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "beat-server"

include(
    "apps:api",
    "apps:admin",
    "apps:batch",
    "application:frontoffice",
    "application:admin",
    "application:system",
    "domain",
    "infrastructure",
    "support:security",
    "support:observability",
)
