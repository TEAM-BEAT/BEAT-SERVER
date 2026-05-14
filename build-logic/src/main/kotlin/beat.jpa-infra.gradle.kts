import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("beat.spring-library")
    kotlin("plugin.jpa")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("spring-boot-core").get())
    implementation(libs.findLibrary("spring-boot-persistence").get())
    compileOnly(libs.findLibrary("spring-boot-starter-data-jpa").get())
}
