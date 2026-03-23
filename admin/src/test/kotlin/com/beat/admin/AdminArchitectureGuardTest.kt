package com.beat.admin

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class AdminArchitectureGuardTest {

    private val rootProjectDependencyPattern = Regex("""project\(\s*":\"\s*\)""")

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
            "com.beat.global.common.scheduler.application.",
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
}
