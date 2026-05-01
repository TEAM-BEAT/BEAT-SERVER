package com.beat.batch

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class BatchArchitectureGuardTest {

    private val forbiddenProjectDependencyPatterns = mapOf(
        "root project" to Regex("""project\(\s*":\"\s*\)"""),
        "apis module" to Regex("""project\(\s*":apis"\s*\)"""),
        "admin module" to Regex("""project\(\s*":admin"\s*\)"""),
        "gateway module" to Regex("""project\(\s*":gateway"\s*\)"""),
    )

    @Test
    fun `batch build file must not depend on forbidden modules`() {
        val buildFile = Files.readString(Path.of("build.gradle.kts"))
        val violations = forbiddenProjectDependencyPatterns
            .filterValues { it.containsMatchIn(buildFile) }
            .keys
            .toList()

        assertTrue(violations.isEmpty(), "Found forbidden project dependencies: ${violations.joinToString(", ")}")
    }

    @Test
    fun `batch main sources must not reference root or forbidden runtime lanes`() {
        val violations = findForbiddenReferences(
            "com.beat.BeatApplication",
            "com.beat.legacyroot.",
            "com.beat.global.common.config.SecurityConfig",
            "com.beat.global.common.config.WebConfig",
            "com.beat.gateway.",
            "com.beat.apis.",
            "com.beat.admin.",
            "com.beat.global.common.scheduler.application.",
            "com.beat.domain.booking.application.TicketCleanupScheduler",
            "com.beat.domain.booking.application.TicketCleanupService",
            "com.beat.domain.promotion.application.PromotionSchedulerService",
            "com.beat.domain.promotion.application.PromotionMaintenanceService",
        )

        assertTrue(
            violations.isEmpty(),
            "Found forbidden batch source references:\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `batch main sources must not declare legacy owner packages`() {
        val violations = findForbiddenReferences(
            "package com.beat.domain.",
            "package com.beat.global.",
        )

        assertTrue(
            violations.isEmpty(),
            "Found legacy owner package declarations:\n${violations.joinToString("\n")}",
        )
    }


    @Test
    fun `batch scheduled entrypoints must live in job packages`() {
        val violations = findForbiddenReferencesOutsideJobPackages(
            Path.of("src/main"),
            "@Scheduled",
            "org.springframework.scheduling.annotation.Scheduled",
            "ApplicationReadyEvent",
            "org.springframework.context.event.EventListener",
        )

        assertTrue(
            violations.isEmpty(),
            "Found scheduled entrypoints outside batch job packages:\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `batch job entrypoints must depend on facade boundary only`() {
        val violations = findForbiddenReferencesInsideJobPackages(
            Path.of("src/main"),
            ".application.",
            "com.beat.domain.",
            "com.beat.infra.",
            "com.beat.contracts.",
        )

        assertTrue(
            violations.isEmpty(),
            "Found job entrypoint references that skip the facade boundary:\n${violations.joinToString("\n")}",
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

    private fun findForbiddenReferencesOutsideJobPackages(
        root: Path,
        vararg forbiddenReferences: String,
    ): List<String> {
        val paths = Files.walk(root)

        return try {
            paths
                .filter(Files::isRegularFile)
                .filter { path -> path.toString().endsWith(".java") || path.toString().endsWith(".kt") }
                .filter { path -> !path.toString().split(path.fileSystem.separator).contains("job") }
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

    private fun findForbiddenReferencesInsideJobPackages(
        root: Path,
        vararg forbiddenReferences: String,
    ): List<String> {
        val paths = Files.walk(root)

        return try {
            paths
                .filter(Files::isRegularFile)
                .filter { path -> path.toString().endsWith(".java") || path.toString().endsWith(".kt") }
                .filter { path -> path.toString().split(path.fileSystem.separator).contains("job") }
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
