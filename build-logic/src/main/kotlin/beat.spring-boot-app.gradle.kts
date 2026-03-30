import org.gradle.api.artifacts.VersionCatalogsExtension
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
    id("beat.kotlin-base")
    id("beat.test")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

configurations.configureEach {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
}

dependencies {
    implementation(libs.findBundle("boot-app-core").get())
    implementation(libs.findLibrary("spring-boot-starter-log4j2").get())
    compileOnly(libs.findLibrary("lombok").get())
    annotationProcessor(libs.findLibrary("lombok").get())
    testImplementation(libs.findLibrary("spring-boot-starter-test").get())
}

tasks.withType<BootRun>().configureEach {
    // Secret imports resolve from the shared repo-level `secret/` directory.
    workingDir = rootDir
}
