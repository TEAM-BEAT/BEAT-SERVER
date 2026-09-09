import org.gradle.api.tasks.compile.JavaCompile
import org.sonarqube.gradle.SonarExtension

plugins {
    id("beat.kotlin-base")
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.kover)
    alias(libs.plugins.spotless)
    id("beat.test")
    id("beat.root-verification")
}

group = "com"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.named<Jar>("jar") {
    description = "Builds the non-executable root coordination artifact."
    enabled = true
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
}


dependencyAnalysis {
    issues {
        all {
            onAny {
                // Keep dependency-analysis advisory while existing advice is classified.
                // Hard-gate timing: after the buildHealth report has only accepted
                // exceptions or fixed findings, change this to fail and remove the
                // ci-pr.yml continue-on-error guard for buildHealth.
                severity("warn")
            }
        }
    }
}

kover {
    reports {
        total {
            xml {
                onCheck.set(true)
            }
            html {
                onCheck.set(false)
            }
        }
    }
}

dependencies {
    // Kover multi-module aggregation — root report가 11모듈 커버리지를 합산 (공식 문서: dependencies { kover(project(":module")) })
    kover(project(":apps:api"))
    kover(project(":apps:admin"))
    kover(project(":apps:batch"))
    kover(project(":application:frontoffice"))
    kover(project(":application:admin"))
    kover(project(":application:system"))
    kover(project(":domain"))
    kover(project(":infrastructure"))
    kover(project(":support:security"))
    kover(project(":support:security-web"))
    kover(project(":support:observability"))
}

val aggregateKoverXmlReport = layout.buildDirectory.file("reports/kover/report.xml")

sonar {
    properties {
        property("sonar.projectKey", "TEAM-BEAT_BEAT-SERVER")
        property("sonar.organization", "team-beat")
        property("sonar.coverage.jacoco.xmlReportPaths", aggregateKoverXmlReport.get().asFile.absolutePath)
    }
}

subprojects {
    extensions.configure<SonarExtension> {
        properties {
            // Root Kover report aggregates every module, but Sonar matches each entry in the owning subproject.
            property("sonar.coverage.jacoco.xmlReportPaths", aggregateKoverXmlReport.get().asFile.absolutePath)
        }
    }
}

tasks.named("sonar") {
    dependsOn(tasks.named("koverXmlReport"))
}

tasks.named("check") {
    dependsOn("verifyTargetModuleGraph")
    dependsOn("verifyJooqContainment")
    dependsOn("verifyModuleBootJars")
    dependsOn("verifyMainResourceTestProfiles")
    dependsOn("verifyMockFrameworkIsNotGlobalDefault")
}
