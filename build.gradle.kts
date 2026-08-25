import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("beat.kotlin-base")
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.kover)
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

tasks.named("check") {
    dependsOn("verifyTargetModuleGraph")
    dependsOn("verifyJooqContainment")
    dependsOn("verifyModuleBootJars")
    dependsOn("verifyMainResourceTestProfiles")
    dependsOn("verifyMockFrameworkIsNotGlobalDefault")
}
