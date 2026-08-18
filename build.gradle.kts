import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.artifacts.ProjectDependency

plugins {
    java
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.kover)
    alias(libs.plugins.dependency.analysis)
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

val targetRuntimeArchiveNames = mapOf(
    ":apps:api" to "apis",
    ":apps:admin" to "admin",
    ":apps:batch" to "batch",
)

val verifyModuleBootJars by tasks.registering {
    group = "verification"
    description = "Builds target executable jars and verifies deploy-compatible archive names."
    dependsOn(targetRuntimeArchiveNames.keys.map { "$it:bootJar" })

    doLast {
        targetRuntimeArchiveNames.forEach { (projectPath, archiveName) ->
            val bootJarOutputs = project(projectPath).tasks.getByName("bootJar").outputs.files.files
            check(bootJarOutputs.any { output -> output.name.startsWith("$archiveName-") }) {
                "$projectPath must produce a deploy-compatible $archiveName-*.jar: $bootJarOutputs"
            }
        }
    }
}

val targetApplicationProjects = setOf(
    ":application:frontoffice",
    ":application:admin",
    ":application:system",
)

val targetExecutableProjects = targetRuntimeArchiveNames.keys
val targetExecutableApplicationLane = mapOf(
    ":apps:api" to ":application:frontoffice",
    ":apps:admin" to ":application:admin",
    ":apps:batch" to ":application:system",
)

val verifyTargetModuleGraph by tasks.registering {
    group = "verification"
    description = "Verifies the target application lanes are present and compile-time isolated."

    doLast {
        val requiredProjects = targetApplicationProjects + setOf(
            ":apps:api",
            ":apps:admin",
            ":apps:batch",
            ":domain",
            ":infrastructure",
            ":support:security",
            ":support:observability",
        )
        check(requiredProjects.all { findProject(it) != null }) {
            "Missing target project(s): ${requiredProjects.filter { findProject(it) == null }}"
        }
        check(project(":domain").configurations.none { configuration ->
            configuration.dependencies.withType(ProjectDependency::class.java).isNotEmpty()
        }) { "domain must not depend on another BEAT project" }
        targetApplicationProjects.forEach { applicationProject ->
            val dependencies = project(applicationProject).configurations
                .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java) }
                .map { dependency -> dependency.path }
                .toSet()
            check(dependencies.intersect(targetApplicationProjects - applicationProject).isEmpty()) {
                "$applicationProject must not depend on another application lane: $dependencies"
            }
        }
        targetExecutableApplicationLane.forEach { (executableProject, allowedApplicationProject) ->
            val dependencies = project(executableProject).configurations
                .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java) }
                .map { dependency -> dependency.path }
                .toSet()
            val forbiddenProjects =
                (targetExecutableProjects - executableProject) +
                    (targetApplicationProjects - allowedApplicationProject)
            check(dependencies.intersect(forbiddenProjects).isEmpty()) {
                "$executableProject has a cross-runtime or wrong-lane dependency: $dependencies"
            }
        }
    }
}

tasks.named("check") {
    dependsOn(verifyTargetModuleGraph)
}

subprojects {
    group = rootProject.group
    version = rootProject.version
    apply(plugin = "com.autonomousapps.dependency-analysis")
}
