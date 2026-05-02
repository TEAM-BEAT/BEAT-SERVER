package com.beat.apis

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class ApisArchitectureGuardTest {

    private val rootProjectDependencyPattern = Regex("""project\(\s*":"\s*\)""")
    private val apiClientBoundaryPathSegments = listOf(
        "/api/",
        "/facade/",
        "/application/dto/",
        "/application/result/",
    )
    private val domainEnumValueImports = arrayOf(
        "com.beat.domain.booking.domain.BookingStatus",
        "com.beat.domain.member.domain.SocialType",
        "com.beat.domain.performance.domain.BankName",
        "com.beat.domain.performance.domain.Genre",
        "com.beat.domain.schedule.domain.ScheduleNumber",
    )
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
    fun `apis dto and event boundaries must not import raw domain models`() {
        val violations = findForbiddenImportsInPaths(
            listOf("/application/dto/"),
            "com.beat.domain.booking.domain.Booking",
            "com.beat.domain.cast.domain.Cast",
            "com.beat.domain.member.domain.Member",
            "com.beat.domain.performance.domain.Performance",
            "com.beat.domain.performanceimage.domain.PerformanceImage",
            "com.beat.domain.promotion.domain.Promotion",
            "com.beat.domain.schedule.domain.Schedule",
            "com.beat.domain.staff.domain.Staff",
            "com.beat.domain.user.domain.Users",
        )

        assertTrue(
            violations.isEmpty(),
            "Found raw domain model imports in apis DTO/event boundaries:\n${violations.joinToString("\n")}"
        )
    }

    @Test
    fun `apis client boundaries must not add domain enum value imports`() {
        val violations = findForbiddenImportsInPaths(
            apiClientBoundaryPathSegments,
            *domainEnumValueImports,
        )
        assertTrue(
            violations.isEmpty(),
            "Found domain enum imports in API client boundaries. Keep domain enum use inside application/domain "
                + "mapping code and expose API-local contracts to clients:\n${
                    violations.joinToString("\n")
                }"
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

    @Test
    fun `apis application services do not expose raw domain models through public methods`() {
        val violations = findPublicMethodReturnTypeViolations(
            Path.of("src/main"),
            listOf(
                "Booking",
                "Cast",
                "Member",
                "Performance",
                "PerformanceImage",
                "Promotion",
                "Schedule",
                "Staff",
                "Users",
            ),
        )

        assertTrue(
            violations.isEmpty(),
            "Found raw domain model return types in apis application service signatures:\n${
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

    private fun findPublicMethodReturnTypeViolations(root: Path, forbiddenReturnTypes: List<String>): List<String> {
        val paths = Files.walk(root)

        return try {
            paths
                .filter { Files.isRegularFile(it) }
                .filter { it.toString().endsWith(".java") || it.toString().endsWith(".kt") }
                .filter { path ->
                    val normalizedPath = path.toString().replace('\\', '/')
                    normalizedPath.contains("/application/")
                        && normalizedPath.endsWith("Service.${path.fileName.toString().substringAfterLast('.')}")
                }
                .toList()
                .flatMap { path ->
                    Files.readAllLines(path)
                        .mapIndexedNotNull { index, line ->
                            val trimmed = line.trimStart()
                            forbiddenReturnTypes
                                .firstOrNull { type ->
                                    trimmed.matches(Regex("""public\s+(?!record\b).*\b$type\b.*\("""))
                                }
                                ?.let { type -> "$path:${index + 1}: $type" }
                        }
                }
        } finally {
            paths.close()
        }
    }

    private fun findForbiddenImportsInPaths(pathSegments: List<String>, vararg forbiddenImports: String): List<String> {
        val paths = Files.walk(Path.of("src/main"))

        return try {
            paths
                .filter { Files.isRegularFile(it) }
                .filter { it.toString().endsWith(".java") || it.toString().endsWith(".kt") }
                .filter { path -> pathSegments.any { segment -> path.toString().replace('\\', '/').contains(segment) } }
                .toList()
                .flatMap { path ->
                    Files.readAllLines(path)
                        .asSequence()
                        .filter { it.trimStart().startsWith("import ") }
                        .flatMap { line ->
                            val normalizedImport = line.trim().removeSuffix(";")
                            forbiddenImports
                                .filter { forbiddenImport -> normalizedImport == "import $forbiddenImport" }
                                .map { forbiddenImport -> "${path}: $forbiddenImport" }
                        }
                        .toList()
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
