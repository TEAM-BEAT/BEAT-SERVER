package com.beat.apis

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class ApisArchitectureGuardTest {

    private val rootProjectDependencyPattern = Regex("""project\(\s*":\"\s*\)""")

    @Test
    fun `apis build file must not depend on root project`() {
        val buildFile = Files.readString(Path.of("build.gradle.kts"))

        assertFalse(rootProjectDependencyPattern.containsMatchIn(buildFile))
    }

    @Test
    fun `apis main sources must not reference root bootstrap lanes`() {
        val forbiddenReferences = listOf(
            "com.beat.BeatApplication",
            "com.beat.legacyroot.",
            "com.beat.batch.",
            "com.beat.global.common.config.SecurityConfig",
            "com.beat.global.common.config.WebConfig",
        )

        val paths = Files.walk(Path.of("src/main"))
        val violations = try {
            paths
                .filter { Files.isRegularFile(it) }
                .filter { it.toString().endsWith(".java") || it.toString().endsWith(".kt") }
                .toList()
                .flatMap { path ->
                    val source = Files.readString(path)
                    forbiddenReferences
                        .filter(source::contains)
                        .map { pattern -> "${path}: $pattern" }
                }
        } finally {
            paths.close()
        }

        assertTrue(violations.isEmpty(), "Found forbidden root bootstrap references:\n${violations.joinToString("\n")}")
    }

    @Test
    fun `apis main sources must not import gateway internals or infra implementations`() {
        val violations = findForbiddenImports(
            "com.beat.gateway.security.",
            "com.beat.gateway.filter.",
            "com.beat.gateway.config.",
            "com.beat.infra.external.",
            ".repository.impl.",
            ".repository.jpa.",
            ".entity.",
        )

        assertTrue(
            violations.isEmpty(),
            "Found forbidden apis source references:\n${violations.joinToString("\n")}"
        )
    }

    @Test
    fun `apis main sources must not declare legacy owner packages`() {
        val violations = findFilesMatching(
            "package com.beat.domain.",
            "package com.beat.global.",
        )

        assertTrue(
            violations.isEmpty(),
            "Found legacy owner package declarations:\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `apis controllers must enter use cases through facades`() {
        val violations = findSourceViolations(
            pathPredicate = { path ->
                path.fileName.toString().matches(Regex(""".*Controller\.(java|kt)"""))
            },
            forbiddenReferencePatterns = listOf(
                Regex("""com\.beat\.apis(?:\.[A-Za-z0-9_]+)+\.application(?:\.[A-Za-z0-9_]+)*\.[A-Za-z0-9_]+Service"""),
                Regex("""com\.beat\.contracts\.(?:[A-Za-z0-9_]+\.)*[A-Za-z0-9_]+Port"""),
            ),
        )

        assertTrue(
            violations.isEmpty(),
            "Controllers must depend on facade entrypoints instead of application services or ports:\n${
                violations.joinToString("\n")
            }"
        )
    }

    @Test
    fun `apis facades must delegate port access to application services`() {
        val violations = findSourceViolations(
            pathPredicate = { path ->
                path.fileName.toString().matches(Regex(""".*Facade\.(java|kt)"""))
            },
            forbiddenReferencePatterns = listOf(
                Regex("""com\.beat\.contracts\.(?:[A-Za-z0-9_]+\.)*[A-Za-z0-9_]+Port"""),
            ),
        )

        assertTrue(
            violations.isEmpty(),
            "Facades must call application services instead of module-contract ports directly:\n${
                violations.joinToString("\n")
            }"
        )
    }

    private fun findSourceViolations(
        pathPredicate: (Path) -> Boolean,
        forbiddenReferencePatterns: List<Regex>,
    ): List<String> {
        val paths = Files.walk(Path.of("src/main"))

        return try {
            paths
                .filter { Files.isRegularFile(it) }
                .filter { it.toString().endsWith(".java") || it.toString().endsWith(".kt") }
                .filter(pathPredicate)
                .toList()
                .flatMap { path ->
                    val source = Files.readString(path)
                    forbiddenReferencePatterns
                        .filter { pattern -> pattern.containsMatchIn(source) }
                        .map { pattern -> "${path}: ${pattern.pattern}" }
                }
        } finally {
            paths.close()
        }
    }

    private fun findForbiddenImports(vararg forbiddenReferences: String): List<String> {
        val paths = Files.walk(Path.of("src/main"))

        return try {
            paths
                .filter { Files.isRegularFile(it) }
                .filter { it.toString().endsWith(".java") || it.toString().endsWith(".kt") }
                .toList()
                .flatMap { path ->
                    Files.readAllLines(path)
                        .asSequence()
                        .filter { it.trimStart().startsWith("import ") }
                        .flatMap { line ->
                            forbiddenReferences
                                .filter(line::contains)
                                .map { pattern -> "${path}: $pattern" }
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
                .filter { Files.isRegularFile(it) }
                .filter { it.toString().endsWith(".java") || it.toString().endsWith(".kt") }
                .toList()
                .flatMap { path ->
                    val source = Files.readString(path)
                    forbiddenReferences
                        .filter(source::contains)
                        .map { pattern -> "${path}: $pattern" }
                }
        } finally {
            paths.close()
        }
    }

    private fun findFilesMatching(vararg forbiddenReferences: String): List<String> {
        val packagePatterns = forbiddenReferences.map { reference -> Regex("""(?m)^\s*${Regex.escape(reference)}""") }
        val paths = Files.walk(Path.of("src/main"))

        return try {
            paths
                .filter { Files.isRegularFile(it) }
                .filter { it.toString().endsWith(".java") || it.toString().endsWith(".kt") }
                .toList()
                .flatMap { path ->
                    val source = Files.readString(path)
                    packagePatterns
                        .filter { pattern -> pattern.containsMatchIn(source) }
                        .map { pattern -> "${path}: ${pattern.pattern}" }
                }
        } finally {
            paths.close()
        }
    }
}
