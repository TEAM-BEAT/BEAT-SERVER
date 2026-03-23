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
    fun `apis main sources must not import root bootstrap lanes`() {
        val forbiddenImports = listOf(
            "import com.beat.BeatApplication",
            "import com.beat.legacyroot.",
            "import com.beat.global.common.scheduler.application.",
            "import com.beat.global.common.config.SecurityConfig",
            "import com.beat.global.common.config.WebConfig",
        )

        val paths = Files.walk(Path.of("src/main"))
        val violations = try {
            paths
                .filter { Files.isRegularFile(it) }
                .filter { it.toString().endsWith(".java") || it.toString().endsWith(".kt") }
                .toList()
                .flatMap { path ->
                    val source = Files.readString(path)
                    forbiddenImports
                        .filter(source::contains)
                        .map { pattern -> "${path}: $pattern" }
                }
        } finally {
            paths.close()
        }

        assertTrue(violations.isEmpty(), "Found forbidden root bootstrap imports:\n${violations.joinToString("\n")}")
    }
}
