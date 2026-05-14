import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("beat.infra-library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    compileOnly(platform(libs.findLibrary("spring-cloud-dependencies").get()))
    compileOnly(libs.findLibrary("spring-boot-starter-web").get())
    compileOnly(libs.findLibrary("spring-cloud-starter-openfeign").get())
}
