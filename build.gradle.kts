import org.gradle.api.tasks.compile.JavaCompile

plugins {
    java
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.kover)
    id("beat.test")
}

group = "com"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Jar>("jar") {
    description = "Builds the non-executable root coordination artifact."
    enabled = true
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
}

fun registerVerificationTask(
    name: String,
    description: String,
    vararg dependencies: Any,
) {
    tasks.register(name) {
        group = "verification"
        this.description = description
        dependsOn(*dependencies)
    }
}

val transitionBoundaryTest by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the root transition boundary guard tests only."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("com.beat.architecture.PromotionBoundaryTest")
        includeTestsMatching("com.beat.RootRetirementContractTest")
        includeTestsMatching("com.beat.SharedBoundaryContractTest")
    }
}

registerVerificationTask(
    "verifyModuleBootJars",
    "Builds boot jars for the current executable modules.",
    ":apis:bootJar",
    ":admin:bootJar",
    ":batch:bootJar",
)

subprojects {
    group = rootProject.group
    version = rootProject.version
}
