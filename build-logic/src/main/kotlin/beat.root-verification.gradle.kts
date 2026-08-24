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
        val mainConfigurations = setOf("api", "implementation", "compileOnly", "runtimeOnly")
        // Allowlist is SSOT: docs/architecture/architecture.md §2.1
        // Any BEAT project dependency not in this map is a CI failure.
        val allowedMainProjectDependencies: Map<String, Set<String>> = mapOf(
            ":domain" to emptySet(),
            ":application:frontoffice" to setOf(":domain", ":support:security"),
            ":application:admin" to setOf(":domain"),
            ":application:system" to setOf(":domain"),
            ":infrastructure" to setOf(
                ":domain",
                ":application:frontoffice",
                ":application:admin",
                ":application:system",
            ),
            ":support:security" to setOf(":support:observability"),
            ":support:observability" to emptySet(),
            ":apps:api" to setOf(
                ":application:frontoffice",
                ":infrastructure",
                ":support:security",
                ":support:observability",
                ":domain", // testImplementation for ArchUnit guards
            ),
            ":apps:admin" to setOf(
                ":application:admin",
                ":infrastructure",
                ":support:security",
                ":support:observability",
                ":domain",
            ),
            ":apps:batch" to setOf(
                ":application:system",
                ":infrastructure",
                ":support:observability",
                ":domain",
            ),
        )
        // Required dependencies — must be present in main configurations
        val requiredMainProjectDependencies: Map<String, Set<String>> = mapOf(
            ":infrastructure" to setOf(":domain", ":application:frontoffice", ":application:admin"),
            ":apps:api" to setOf(":application:frontoffice"),
            ":apps:admin" to setOf(":application:admin"),
            ":apps:batch" to setOf(":application:system"),
        )
        // Enforce allowlist: actual ⊆ allowed
        canonicalTargetLeafProjects.forEach { projectPath ->
            val actual = projectDependenciesOf(projectPath)
            val allowed = allowedMainProjectDependencies[projectPath] ?: emptySet()
            val unexpected = actual - allowed
            check(unexpected.isEmpty()) {
                "$projectPath has unexpected BEAT project dependencies not in allowlist $allowed: $unexpected (actual: $actual)"
            }
        }
        // Enforce required: required ⊆ actual (in main configurations)
        requiredMainProjectDependencies.forEach { (projectPath, required) ->
            val actualMain = project(projectPath).configurations
                .filter { it.name in mainConfigurations }
                .flatMap { it.dependencies.withType(ProjectDependency::class.java) }
                .map { it.path }.toSet()
            val missing = required - actualMain
            check(missing.isEmpty()) {
                "$projectPath is missing required BEAT project dependencies $required (actual main: $actualMain)"
            }
        }
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
            val mainDependencies = project(executableProject).configurations
                .filter { configuration -> configuration.name in mainConfigurations }
                .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java) }
                .map { dependency -> dependency.path }
                .toSet()
            check(allowedApplicationProject in mainDependencies) {
                "$executableProject must depend on its designated application lane $allowedApplicationProject: $mainDependencies"
            }
            val forbiddenProjects =
                (targetExecutableProjects - executableProject) +
                    (targetApplicationProjects - allowedApplicationProject)
            check(dependencies.intersect(forbiddenProjects).isEmpty()) {
                "$executableProject has a cross-runtime or wrong-lane dependency: $dependencies"
            }
        }
    }
}
