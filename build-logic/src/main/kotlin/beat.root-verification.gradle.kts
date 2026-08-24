import org.gradle.api.artifacts.ProjectDependency

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
        val mainConfigurations = setOf("api", "implementation", "compileOnly", "runtimeOnly")
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
