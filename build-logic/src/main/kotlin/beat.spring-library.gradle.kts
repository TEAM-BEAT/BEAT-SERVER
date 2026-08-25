import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("beat.library")
    id("beat.test")
    kotlin("plugin.spring")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(platform(libs.findLibrary("spring-boot-dependencies").get()))
}
