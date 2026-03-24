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
    compileOnly(libs.findLibrary("spring-boot-starter-data-redis").get())
    compileOnly(libs.findLibrary("spring-security-core").get())
    compileOnly(libs.findLibrary("querydsl-jpa-jakarta").get()) {
        artifact {
            classifier = "jakarta"
        }
    }
}
