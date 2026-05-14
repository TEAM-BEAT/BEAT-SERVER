import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("beat.spring-boot-app")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(platform(libs.findLibrary("spring-cloud-dependencies").get()))
    implementation(libs.findBundle("web-app-core").get())
}
