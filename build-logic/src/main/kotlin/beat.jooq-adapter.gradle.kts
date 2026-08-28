import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("beat.infra-library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("spring-boot-starter-jooq").get())
}
