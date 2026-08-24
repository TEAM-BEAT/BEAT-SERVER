import org.gradle.api.artifacts.ProjectDependency

val targetRuntimeArchiveNames = mapOf(
    ":apps:api" to "apis",
    ":apps:admin" to "admin",
    ":apps:batch" to "batch",
)

val canonicalTargetLeafProjects = setOf(
    ":apps:api",
    ":apps:admin",
    ":apps:batch",
    ":application:frontoffice",
    ":application:admin",
    ":application:system",
    ":domain",
    ":infrastructure",
    ":support:security",
    ":support:observability",
)
val canonicalTargetLibraryProjects = canonicalTargetLeafProjects - targetRuntimeArchiveNames.keys

val verifyModuleBootJars by tasks.registering {
    group = "verification"
    description = "Builds target executable jars and verifies deploy-compatible archive names."
    dependsOn(targetRuntimeArchiveNames.keys.map { "$it:bootJar" })

    doLast {
        targetRuntimeArchiveNames.forEach { (projectPath, archiveName) ->
            val bootJar = project(projectPath).tasks.getByName("bootJar")
            check(bootJar.enabled) {
                "$projectPath must have an enabled bootJar task"
            }
            val expectedOutputs = bootJar.outputs.files.files
                .filter { output -> output.name.startsWith("$archiveName-") }
            check(expectedOutputs.any { output -> output.isFile }) {
                "$projectPath must produce a deploy-compatible $archiveName-*.jar file: expected outputs $expectedOutputs"
            }
        }
        canonicalTargetLibraryProjects.forEach { projectPath ->
            val bootJar = project(projectPath).tasks.findByName("bootJar")
            check(bootJar == null || !bootJar.enabled) {
                "$projectPath must not have an enabled bootJar task"
            }
        }
    }
}

val verifyMockFrameworkIsNotGlobalDefault by tasks.registering {
    group = "verification"
    description = "Forbids MockK from becoming the global test convention default."

    val testConventionFile = rootProject.file("build-logic/src/main/kotlin/beat.test.gradle.kts")
    inputs.file(testConventionFile)

    doLast {
        check("mockk" !in testConventionFile.readText().lowercase()) {
            "beat.test.gradle.kts must not make MockK a global test default"
        }
    }
}

val verifyMainResourceTestProfiles by tasks.registering {
    group = "verification"
    description = "Forbids test profile overrides in production resources except shared observability defaults."

    val mainYamlFiles = files(
        rootProject.allprojects.map { candidateProject ->
            candidateProject.fileTree("src/main/resources") {
                include("**/*.yml", "**/*.yaml")
            }
        },
    ).filter { resource ->
        resource.relativeTo(rootDir).invariantSeparatorsPath !=
            "support/observability/src/main/resources/application-observability.yml"
    }
    inputs.files(mainYamlFiles)

    doLast {
        val testProfilePattern = Regex("""(?m)^\s*on-profile\s*:\s*.*\btest\b.*$""")
        val offenders = mainYamlFiles.files
            .filter { resource -> testProfilePattern.containsMatchIn(resource.readText()) }
            .map { resource -> resource.relativeTo(rootDir).invariantSeparatorsPath }
            .sorted()

        check(offenders.isEmpty()) {
            "Test profile overrides belong in module-local src/test/resources/application-test.yml: $offenders"
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
        val actualLeafProjects = rootProject.subprojects
            .filter { project -> project.childProjects.isEmpty() }
            .map { project -> project.path }
            .toSet()
        val missingLeafProjects = canonicalTargetLeafProjects - actualLeafProjects
        val unexpectedLeafProjects = actualLeafProjects - canonicalTargetLeafProjects
        check(actualLeafProjects == canonicalTargetLeafProjects) {
            "Target leaf project set mismatch. Missing: $missingLeafProjects; Unexpected: $unexpectedLeafProjects"
        }
        val projectDependenciesOf: (String) -> Set<String> = { projectPath ->
            project(projectPath).configurations
                .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java) }
                .map { dependency -> dependency.path }
                .toSet()
        }
        check(projectDependenciesOf(":domain").isEmpty()) {
            "domain must not depend on another BEAT project"
        }
        targetApplicationProjects.forEach { applicationProject ->
            val dependencies = projectDependenciesOf(applicationProject)
            val forbiddenProjects =
                (targetApplicationProjects - applicationProject) +
                    setOf(":infrastructure") +
                    targetExecutableProjects
            check(dependencies.intersect(forbiddenProjects).isEmpty()) {
                "$applicationProject must not depend on another application lane, infrastructure, or an executable app: $dependencies"
            }
        }
        val infrastructureDependencies = projectDependenciesOf(":infrastructure")
        check(infrastructureDependencies.intersect(targetExecutableProjects).isEmpty()) {
            ":infrastructure must not depend on an executable app: $infrastructureDependencies"
        }
        val forbiddenSupportSecurityProjects =
            targetApplicationProjects + setOf(":infrastructure") + targetExecutableProjects
        check(projectDependenciesOf(":support:security").intersect(forbiddenSupportSecurityProjects).isEmpty()) {
            ":support:security must not depend on an application lane, infrastructure, or an executable app"
        }
        val forbiddenSupportObservabilityProjects =
            targetApplicationProjects +
                setOf(":domain", ":infrastructure", ":support:security") +
                targetExecutableProjects
        check(projectDependenciesOf(":support:observability").intersect(forbiddenSupportObservabilityProjects).isEmpty()) {
            ":support:observability must not depend on domain, an application lane, infrastructure, an executable app, or support:security"
        }
        val mainConfigurations = setOf("api", "implementation", "compileOnly", "runtimeOnly")
        val forbiddenDomainExternalDependencies = project(":domain").configurations
            .filter { configuration -> configuration.name in mainConfigurations }
            .flatMap { configuration -> configuration.dependencies }
            .filter { dependency ->
                if (dependency is ProjectDependency) {
                    false
                } else {
                    val group = dependency.group.orEmpty()
                    val module = dependency.name.lowercase()
                    group.startsWith("org.springframework") ||
                        group.startsWith("jakarta.persistence") ||
                        group.startsWith("org.hibernate") ||
                        group.startsWith("org.redisson") ||
                        module.contains("redis") ||
                        module.contains("web") ||
                        module.contains("jpa")
                }
            }
            .map { dependency -> "${dependency.group}:${dependency.name}" }
            .toSet()
        check(forbiddenDomainExternalDependencies.isEmpty()) {
            "domain must not depend directly on framework, persistence, Redis, web, or JPA external modules: " +
                forbiddenDomainExternalDependencies
        }
        targetExecutableApplicationLane.keys.forEach { executableProject ->
            val mainDependencies = project(executableProject).configurations
                .filter { configuration -> configuration.name in mainConfigurations }
                .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java) }
                .map { dependency -> dependency.path }
                .toSet()
            check(":domain" !in mainDependencies) {
                "$executableProject must not depend directly on :domain in main configurations: $mainDependencies"
            }
        }
        targetExecutableApplicationLane.forEach { (executableProject, allowedApplicationProject) ->
            val dependencies = projectDependenciesOf(executableProject)
            val forbiddenProjects =
                (targetExecutableProjects - executableProject) +
                    (targetApplicationProjects - allowedApplicationProject)
            check(dependencies.intersect(forbiddenProjects).isEmpty()) {
                "$executableProject has a cross-runtime or wrong-lane dependency: $dependencies"
            }
        }
    }
}
