package com.beat.admin

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class AdminArchitectureGuardTest {

    private val rootProjectDependencyPattern = Regex("""project\(\s*":"\s*\)""")

    @Test
    fun `admin build file must not depend on root project`() {
        val buildFile = Files.readString(Path.of("build.gradle.kts"))

        assertFalse(rootProjectDependencyPattern.containsMatchIn(buildFile))
    }

    @Test
    fun `admin sources must not reference root bootstrap lanes`() {
        val violations = findForbiddenReferences(
            "com.beat.BeatApplication",
            "com.beat.legacyroot.",
            "com.beat.batch.",
            "com.beat.global.common.config.SecurityConfig",
            "com.beat.global.common.config.WebConfig",
        )

        assertTrue(violations.isEmpty(), "Found forbidden root bootstrap references:\n${violations.joinToString("\n")}")
    }

    @Test
    fun `admin sources must not import gateway internal packages`() {
        val violations = findForbiddenReferences(
            "com.beat.gateway.security.",
            "com.beat.gateway.filter.",
            "com.beat.gateway.config.",
        )

        assertTrue(
            violations.isEmpty(),
            "Found forbidden gateway internal references:\n${violations.joinToString("\n")}"
        )
    }

    @Test
    fun `admin sources must not import infra implementation packages`() {
        val violations = findForbiddenImports(
            "com.beat.infra.external.",
            ".repository.impl.",
            ".repository.jpa.",
            ".entity.",
        )

        assertTrue(
            violations.isEmpty(),
            "Found forbidden infra implementation references:\n${violations.joinToString("\n")}"
        )
    }

    @Test
    fun `admin sources must not declare legacy owner packages`() {
        val violations = findPackageDeclarationViolations(
            "package com.beat.domain.",
            "package com.beat.global.",
        )

        assertTrue(
            violations.isEmpty(),
            "Found legacy owner package declarations:\n${violations.joinToString("\n")}"
        )
    }

    @Test
    fun `admin facades must not own transaction repository or raw domain model dependencies`() {
        val violations = findSourceViolationsInMatchingPaths(
            Path.of("src/main/java/com/beat/admin"),
            pathSegment = "/facade/",
            "@Transactional",
            "com.beat.domain.",
            "Repository",
        )

        assertTrue(
            violations.isEmpty(),
            "Found forbidden facade dependencies:\n${violations.joinToString("\n")}"
        )
    }

    @Test
    fun `admin does not keep transitional port in package`() {
        val portIn = Path.of("src/main/java/com/beat/admin/port/in")

        assertFalse(Files.exists(portIn), "admin port/in package should not remain after facade-application cleanup")
    }

    @Test
    fun `admin does not keep transitional adapter controller or root layer files`() {
        val forbiddenPaths = listOf(
            Path.of("src/main/java/com/beat/admin/adapter"),
            Path.of("src/main/java/com/beat/admin/controller"),
            Path.of("src/main/java/com/beat/admin/api/AdminApi.java"),
            Path.of("src/main/java/com/beat/admin/api/AdminController.java"),
            Path.of("src/main/java/com/beat/admin/facade/AdminFacade.java"),
            Path.of("src/main/java/com/beat/admin/application/service"),
        )

        val violations = forbiddenPaths.filter(Files::exists)

        assertTrue(
            violations.isEmpty(),
            "admin transitional package/file should not remain after context package split: ${violations.joinToString(", ")}"
        )
    }

    @Test
    fun `admin success code belongs to api response boundary`() {
        assertTrue(Files.exists(Path.of("src/main/kotlin/com/beat/admin/api/response/AdminSuccessCode.kt")))
        assertFalse(Files.exists(Path.of("src/main/java/com/beat/admin/exception/AdminSuccessCode.java")))
    }

    @Test
    fun `admin dto layer must not import domain types`() {
        val violations = findForbiddenImportsInMatchingPaths(
            Path.of("src/main/java/com/beat/admin"),
            pathSegment = "/dto/",
            "com.beat.domain."
        )

        assertTrue(
            violations.isEmpty(),
            "Found domain type imports in admin DTO layer:\n${violations.joinToString("\n")}"
        )
    }

    @Test
    fun `admin application services do not return raw domain models`() {
        val violations = findMethodSignatureViolations(
            Path.of("src/main/java/com/beat/admin"),
            listOf("Promotion", "Users")
        )

        assertTrue(
            violations.isEmpty(),
            "Found raw domain model return types in admin application service signatures:\n${violations.joinToString("\n")}"
        )
    }

    private fun findMethodSignatureViolations(root: Path, forbiddenReturnTypes: List<String>): List<String> {
        val paths = Files.walk(root)

        return try {
            paths
                .filter(Files::isRegularFile)
                .filter { path -> path.toString().endsWith(".java") || path.toString().endsWith(".kt") }
                .toList()
                .flatMap { path ->
                    Files.readAllLines(path)
                        .mapIndexedNotNull { index, line ->
                            val trimmed = line.trimStart()
                            forbiddenReturnTypes
                                .firstOrNull { type -> trimmed.matches(Regex("""public\s+(?!record\b).*\b$type\b.*\(""")) }
                                ?.let { type -> "$path:${index + 1}: $type" }
                        }
                }
        } finally {
            paths.close()
        }
    }

    private fun findForbiddenImportsInMatchingPaths(
        root: Path,
        pathSegment: String,
        vararg forbiddenReferences: String,
    ): List<String> {
        if (!Files.exists(root)) {
            return emptyList()
        }

        val paths = Files.walk(root)

        return try {
            paths
                .filter(Files::isRegularFile)
                .filter { path -> path.toString().contains(pathSegment) }
                .filter { path -> path.toString().endsWith(".java") || path.toString().endsWith(".kt") }
                .toList()
                .flatMap { path ->
                    Files.readAllLines(path)
                        .asSequence()
                        .filter { it.trimStart().startsWith("import ") }
                        .flatMap { line ->
                            forbiddenReferences
                                .filter(line::contains)
                                .map { pattern -> "$path: $pattern" }
                        }
                        .toList()
                }
        } finally {
            paths.close()
        }
    }

    private fun findSourceViolationsInMatchingPaths(
        root: Path,
        pathSegment: String,
        vararg forbiddenReferences: String,
    ): List<String> {
        val paths = Files.walk(root)

        return try {
            paths
                .filter(Files::isRegularFile)
                .filter { path -> path.toString().contains(pathSegment) }
                .filter { path -> path.toString().endsWith(".java") || path.toString().endsWith(".kt") }
                .toList()
                .flatMap { path ->
                    val source = Files.readString(path)
                    forbiddenReferences
                        .filter(source::contains)
                        .map { pattern -> "$path: $pattern" }
                }
        } finally {
            paths.close()
        }
    }

    private fun findForbiddenImports(vararg forbiddenReferences: String): List<String> {
        val paths = Files.walk(Path.of("src/main"))

        return try {
            paths
                .filter(Files::isRegularFile)
                .filter { path -> path.toString().endsWith(".java") || path.toString().endsWith(".kt") }
                .toList()
                .flatMap { path ->
                    Files.readAllLines(path)
                        .asSequence()
                        .filter { it.trimStart().startsWith("import ") }
                        .flatMap { line ->
                            forbiddenReferences
                                .filter(line::contains)
                                .map { pattern -> "$path: $pattern" }
                        }
                        .toList()
                }
        } finally {
            paths.close()
        }
    }

    private fun findForbiddenReferences(vararg forbiddenReferences: String): List<String> {
        val paths = Files.walk(Path.of("src/main"))

        return try {
            paths
                .filter(Files::isRegularFile)
                .filter { path -> path.toString().endsWith(".java") || path.toString().endsWith(".kt") }
                .toList()
                .flatMap { path ->
                    val source = Files.readString(path)
                    forbiddenReferences
                        .filter(source::contains)
                        .map { pattern -> "$path: $pattern" }
                }
        } finally {
            paths.close()
        }
    }

    private fun findPackageDeclarationViolations(vararg forbiddenReferences: String): List<String> {
        val paths = Files.walk(Path.of("src/main"))

        return try {
            paths
                .filter(Files::isRegularFile)
                .filter { path -> path.toString().endsWith(".java") || path.toString().endsWith(".kt") }
                .toList()
                .flatMap { path ->
                    val source = Files.readString(path)
                    forbiddenReferences
                        .filter(source::startsWith)
                        .map { pattern -> "$path: $pattern" }
                }
        } finally {
            paths.close()
        }
    }
}
