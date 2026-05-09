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

extra["tomcat.version"] = libs.findVersion("tomcat-embed").get().requiredVersion
extra["jackson-bom.version"] = libs.findVersion("jackson3").get().requiredVersion
extra["netty.version"] = libs.findVersion("netty").get().requiredVersion

configurations.configureEach {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
}

dependencies {
    implementation(libs.findBundle("boot-app-core").get())
    implementation(libs.findLibrary("spring-boot-starter-log4j2").get())
    compileOnly(libs.findLibrary("lombok").get())
    annotationProcessor(libs.findLibrary("lombok").get())
    testImplementation(libs.findLibrary("spring-boot-starter-test").get())

    constraints {
        implementation(libs.findLibrary("tomcat-embed-core").get()) {
            because("Trivy reports fixed Tomcat runtime CVEs against the Spring Boot managed 11.0.20 baseline")
        }
        implementation(libs.findLibrary("tomcat-embed-el").get()) {
            because("Keep embedded Tomcat artifacts aligned after the CVE-driven core override")
        }
        implementation(libs.findLibrary("tomcat-embed-websocket").get()) {
            because("Keep embedded Tomcat artifacts aligned after the CVE-driven core override")
        }
        implementation(libs.findLibrary("jackson3-core").get()) {
            because("Trivy reports GHSA-2m67-wjpj-xhg9 against the Spring Boot managed Jackson 3.1.0 baseline")
        }
        implementation(libs.findLibrary("jackson3-databind").get()) {
            because("Keep Jackson 3 artifacts aligned after the CVE-driven core override")
        }
        implementation(libs.findLibrary("commons-fileupload").get()) {
            because("Trivy reports CVE-2025-48976 against the OpenFeign form transitive 1.5 baseline")
        }
        implementation(libs.findLibrary("netty-codec-dns").get()) {
            because("Trivy reports CVE-2026-42579 against the transitive Netty 4.2.12.Final DNS codec baseline")
        }
        implementation(libs.findLibrary("netty-resolver-dns").get()) {
            because("Keep Netty DNS artifacts aligned after the CVE-driven codec-dns override")
        }
        implementation(libs.findLibrary("bouncycastle-bcprov-jdk18on").get()) {
            because("Trivy reports CVE-2026-5598 against the transitive Bouncy Castle 1.81 baseline")
        }
    }
}

tasks.withType<BootRun>().configureEach {
    // Secret imports resolve from the shared repo-level `secret/` directory.
    workingDir = rootDir
}
