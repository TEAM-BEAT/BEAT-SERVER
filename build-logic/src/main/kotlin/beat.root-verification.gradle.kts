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
    ":support:security-core",
    ":support:security-web",
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

// ── POLICY — single source of truth, SSOT §2.1 ──
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
val mainConfigurations = setOf("api", "implementation", "compileOnly", "runtimeOnly")

// Allowed = any configuration (including test) may depend on these; anything else is CI failure
val allowedProjectDependencies: Map<String, Set<String>> = mapOf(
    ":domain" to emptySet(),
    ":application:frontoffice" to setOf(":domain"),
    ":application:admin" to setOf(":domain"),
    ":application:system" to setOf(":domain"),
    ":infrastructure" to setOf(
        ":domain",
        ":application:frontoffice",
        ":application:admin",
        ":application:system",
    ),
    ":support:security-core" to setOf(":application:frontoffice", ":support:observability"),
    ":support:security-web" to setOf(":application:frontoffice", ":support:security-core", ":support:observability"),
    ":support:observability" to emptySet(),
    ":apps:api" to setOf(
        ":application:frontoffice",
        ":infrastructure",
        ":support:security-web",
        ":support:observability",
        ":domain", // testImplementation for ArchUnit guards
    ),
    ":apps:admin" to setOf(
        ":application:admin",
        ":infrastructure",
        ":support:security-web",
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
// Required = must be present in main (runtime) configurations
val requiredMainProjectDependencies: Map<String, Set<String>> = mapOf(
    ":infrastructure" to setOf(":domain", ":application:frontoffice", ":application:admin"),
    ":apps:api" to setOf(":application:frontoffice"),
    ":apps:admin" to setOf(":application:admin"),
    ":apps:batch" to setOf(":application:system"),
)

val verifyTargetModuleGraph by tasks.registering {
    group = "verification"
    description = "Verifies the target module graph is exactly the SSOT allowlist."

    doLast {
        // ── HELPERS ──
        val allProjectDependenciesOf: (String) -> Set<String> = { projectPath ->
            project(projectPath).configurations
                .flatMap { it.dependencies.withType(ProjectDependency::class.java) }
                .map { it.path }.toSet()
        }
        val mainProjectDependenciesOf: (String) -> Set<String> = { projectPath ->
            project(projectPath).configurations
                .filter { it.name in mainConfigurations }
                .flatMap { it.dependencies.withType(ProjectDependency::class.java) }
                .map { it.path }.toSet()
        }

        // ── CHECKS ──
        // 1. Exact leaf projects
        val actualLeafProjects = rootProject.subprojects
            .filter { it.childProjects.isEmpty() }.map { it.path }.toSet()
        check(actualLeafProjects == canonicalTargetLeafProjects) {
            "Target leaf project set mismatch. Missing: ${canonicalTargetLeafProjects - actualLeafProjects}; Unexpected: ${actualLeafProjects - canonicalTargetLeafProjects}"
        }

        // 2. Allowlist: actual ⊆ allowed (all configurations)
        canonicalTargetLeafProjects.forEach { projectPath ->
            val actual = allProjectDependenciesOf(projectPath)
            val allowed = allowedProjectDependencies[projectPath] ?: emptySet()
            val unexpected = actual - allowed
            check(unexpected.isEmpty()) {
                "$projectPath has unexpected BEAT project dependencies not in allowlist $allowed: $unexpected (actual: $actual)"
            }
        }

        // 3. Required: required ⊆ actualMain (main configurations)
        requiredMainProjectDependencies.forEach { (projectPath, required) ->
            val actualMain = mainProjectDependenciesOf(projectPath)
            val missing = required - actualMain
            check(missing.isEmpty()) {
                "$projectPath is missing required BEAT project dependencies $required (actual main: $actualMain)"
            }
        }

        // 4. Domain external tech ban
        val forbiddenDomainExternalDependencies = project(":domain").configurations
            .filter { it.name in mainConfigurations }
            .flatMap { it.dependencies }
            .filter { dep ->
                if (dep is ProjectDependency) false else {
                    val g = dep.group.orEmpty(); val m = dep.name.lowercase()
                    g.startsWith("org.springframework") || g.startsWith("jakarta.persistence") ||
                        g.startsWith("org.hibernate") || g.startsWith("org.redisson") ||
                        m.contains("redis") || m.contains("web") || m.contains("jpa")
                }
            }.map { "${it.group}:${it.name}" }.toSet()
        check(forbiddenDomainExternalDependencies.isEmpty()) {
            "domain must not depend directly on framework, persistence, Redis, web, or JPA: $forbiddenDomainExternalDependencies"
        }

        // 5. Apps must not depend directly on domain in main (even if allowlist permits test)
        targetExecutableApplicationLane.keys.forEach { exe ->
            check(":domain" !in mainProjectDependenciesOf(exe)) {
                "$exe must not depend directly on :domain in main configurations: ${mainProjectDependenciesOf(exe)}"
            }
        }
    }
}
