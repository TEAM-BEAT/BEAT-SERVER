import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("beat.spring-library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("spring-boot-core").get())
}
