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
        "/application/result/",
    )
    private val domainEnumValueImports = arrayOf(
        "com.beat.domain.booking.model.BookingStatus",
        "com.beat.domain.member.model.SocialType",
        "com.beat.domain.sharedkernel.vo.BankName",
        "com.beat.domain.performance.model.Genre",
        "com.beat.domain.schedule.model.ScheduleNumber",
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
            "com.beat.global.common.config.",
            "com.beat.global.support.config.",
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
    fun `apis main sources import only public gateway boundary and no infra implementations`() {
        val gatewayViolations = findGatewayImportViolations(
            setOf(
                "com.beat.support.security.EnableGatewayConfig",
                "com.beat.support.security.GatewayConfigGroup",
                "com.beat.support.security.CurrentMember",
                "com.beat.support.security.EnableGatewayServletSecurity",
            )
        )
        val infraViolations = findForbiddenImports(
            "com.beat.infra.external.",
            ".repository.impl.",
            ".repository.jpa.",
            ".entity.",
        )
        val violations = gatewayViolations + infraViolations

        assertTrue(
            violations.isEmpty(),
            "Found forbidden apis source references:\n${violations.joinToString("\n")}"
        )
    }

    @Test
    fun `apis transport result and event boundaries must not import raw domain models`() {
        val violations = findForbiddenImportsInPaths(
            listOf("/api/request/", "/api/response/", "/application/result/", "/application/event/"),
            "com.beat.domain.booking.model.Booking",
            "com.beat.domain.performance.model.Cast",
            "com.beat.domain.member.model.Member",
            "com.beat.domain.performance.model.Performance",
            "com.beat.domain.performance.model.PerformanceImage",
            "com.beat.domain.promotion.model.Promotion",
            "com.beat.domain.schedule.model.Schedule",
            "com.beat.domain.performance.model.Staff",
            "com.beat.domain.user.model.Users",
        )

        assertTrue(
            violations.isEmpty(),
            "Found raw domain model imports in apis DTO/event boundaries:\n${violations.joinToString("\n")}"
        )
    }

    @Test
    fun `apis http dto packages belong to api adapters`() {
        val paths = Files.walk(Path.of("src/main"))
        val violations = try {
            paths
                .filter(Files::isRegularFile)
                .map { it.toString().replace('\\', '/') }
                .filter { "/application/dto/" in it }
                .toList()
        } finally {
            paths.close()
        }

        assertTrue(
            violations.isEmpty(),
            "HTTP DTOs must be placed under api/request or api/response, never application/dto:\n${
                violations.joinToString("\n")
            }",
        )
    }

    @Test
    fun `apis application code must not depend on http request or response dto`() {
        val violations = findSourceViolations(
            pathPredicate = { path -> path.toString().replace('\\', '/').contains("/application/") },
            forbiddenReferencePatterns = listOf(
                Regex("""com\.beat\.apis\.[\w.]+\.api\.request\."""),
                Regex("""com\.beat\.apis\.[\w.]+\.api\.response\."""),
            ),
        )

        assertTrue(
            violations.isEmpty(),
            "Application code must expose Command, Query, and Result models instead of HTTP DTOs:\n${
                violations.joinToString("\n")
            }",
        )
    }

    @Test
    fun `apis controllers validate every request body`() {
        val violations = findSourceViolations(
            pathPredicate = { path ->
                path.fileName.toString().matches(Regex(""".*Controller\.(java|kt)"""))
            },
            forbiddenReferencePatterns = listOf(Regex("""(?m)^(?!.*@Valid).*@RequestBody.*$""")),
        )

        assertTrue(
            violations.isEmpty(),
            "Every controller RequestBody parameter must use @Valid:\n${violations.joinToString("\n")}",
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
    fun `apis java production sources contain only v1 controllers and api interfaces`() {
        val javaRoot = Path.of("src/main/java")
        if (!Files.exists(javaRoot)) {
            return
        }
        val paths = Files.walk(javaRoot)
        val violations = try {
            paths
                .filter(Files::isRegularFile)
                .filter { it.fileName.toString().endsWith(".java") }
                .filter {
                    val name = it.fileName.toString()
                    !name.endsWith("Controller.java") && !name.endsWith("Api.java")
                }
                .toList()
        } finally {
            paths.close()
        }

        assertTrue(
            violations.isEmpty(),
            "Java production sources are limited to legacy V1 controllers and API interfaces:\n${
                violations.joinToString("\n")
            }",
        )
    }

    @Test
    fun `apis controllers must enter use cases through facades`() {
        val violations = findSourceViolations(
            pathPredicate = { path ->
                path.fileName.toString().matches(Regex(""".*Controller\.(java|kt)"""))
            },
            forbiddenReferencePatterns = listOf(
                Regex("""import\s+com\.beat\.apis\.[^\r\n]+\.application(?:\.|;)"""),
                Regex("""com\.beat\.apis(?:\.[A-Za-z0-9_]+)+\.application(?:\.[A-Za-z0-9_]+)*\.[A-Za-z0-9_]+Service"""),
                Regex("""com\.beat\.contracts\.(?:[A-Za-z0-9_]+\.)*[A-Za-z0-9_]+Port"""),
                Regex("""com\.beat\.infra\."""),
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
                Regex("""com\.beat\.domain\.(?:[A-Za-z0-9_]+\.)*repository\."""),
                Regex("""com\.beat\.domain\.(?:[A-Za-z0-9_]+\.)*service\.[A-Za-z0-9_]+DomainService"""),
                Regex("""org\.springframework\.transaction\.annotation\.Transactional"""),
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
    fun `apis application services receive domain services through dependency injection`() {
        val violations = findSourceViolations(
            pathPredicate = { path -> path.toString().replace('\\', '/').contains("/application/") },
            forbiddenReferencePatterns = listOf(
                Regex("""\bnew\s+[A-Za-z0-9_]+DomainService\s*\("""),
                Regex("""(?m)(?<!fun\s)\b[A-Za-z0-9_]+DomainService\s*\(\s*\)"""),
            ),
        )

        assertTrue(
            violations.isEmpty(),
            "Application services must receive DomainService instances from the composition root:\n${
                violations.joinToString("\n")
            }",
        )
    }

    @Test
    fun `apis application services do not call other application services`() {
        val violations = findSourceViolations(
            pathPredicate = { path -> path.toString().replace('\\', '/').contains("/application/") },
            forbiddenReferencePatterns = listOf(
                Regex("""import\s+com\.beat\.apis\.[\w.]+\.application\.(?:command|query)\.[A-Za-z0-9_]+Service"""),
            ),
        )

        assertTrue(
            violations.isEmpty(),
            "Application services must use ports, repositories, or internal collaborators instead of other services:\n${
                violations.joinToString("\n")
            }",
        )
    }

    @Test
    fun `apis application code receives time through Clock`() {
        val violations = findSourceViolations(
            pathPredicate = { path -> path.toString().replace('\\', '/').contains("/application/") },
            forbiddenReferencePatterns = listOf(
                Regex("""\b(?:LocalDate|LocalDateTime|Instant)\.now\(\s*\)"""),
            ),
        )

        assertTrue(violations.isEmpty(), "Application code must not read the system clock directly:\n${violations.joinToString("\n")}")
    }

    @Test
    fun `apis packages use business capability names instead of adapter technology names`() {
        val violations = findForbiddenReferences(
            "package com.beat.apis.external.s3",
            "package com.beat.apis.external.sms",
            "package com.beat.apis.external.image",
            "package com.beat.apis.external.notification.slack",
        )

        assertTrue(violations.isEmpty(), "Provider-specific packages belong to infra adapters:\n${violations.joinToString("\n")}")
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
                    val source = Files.readString(path)
                    forbiddenReturnTypes.flatMap { type ->
                        forbiddenReturnTypeMatches(source, type)
                            .map { match -> "$path:${lineNumberAt(source, match.range.first)}: $type" }
                    }
                }
        } finally {
            paths.close()
        }
    }

    private fun forbiddenReturnTypeMatches(source: String, type: String): Sequence<MatchResult> {
        val escapedType = Regex.escape(type)
        val javaPublicMethod = Regex(
            """(?m)^[ \t]*public\s+(?!record\b)(?:(?:static|final|synchronized|abstract|default|native)\s+)*[\w<>,.? \[\]\r\n\t]*\b$escapedType\b[\w<>,.? \[\]\r\n\t]*\s+\w+\s*\([^;{}]*\)\s*(?:throws\s+[^;{]+)?[;{]"""
        )
        val kotlinPublicFunction = Regex(
            """(?m)^[ \t]*(?!private\b|protected\b|internal\b)(?:public\s+)?(?:suspend\s+)?fun\s+\w+\s*\([^)]*\)\s*:\s*[\w<>,.? \[\]\r\n\t]*\b$escapedType\b[\w<>,.? \[\]\r\n\t]*(?:\s|=|\{)"""
        )

        return javaPublicMethod.findAll(source) + kotlinPublicFunction.findAll(source)
    }

    private fun lineNumberAt(source: String, offset: Int): Int =
        source.take(offset).count { it == '\n' } + 1

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
                                .filter { forbiddenImport -> matchesForbiddenImport(normalizedImport, forbiddenImport) }
                                .map { forbiddenImport -> "${path}: $forbiddenImport" }
                        }
                        .toList()
                }
        } finally {
            paths.close()
        }
    }

    private fun matchesForbiddenImport(normalizedImport: String, forbiddenImport: String): Boolean {
        val importPattern = Regex("""^import\s+${Regex.escape(forbiddenImport)}(?:\s+as\s+\w+)?$""")
        return importPattern.matches(normalizedImport)
    }

    private fun findGatewayImportViolations(allowedImports: Set<String>): List<String> {
        val paths = Files.walk(Path.of("src/main"))

        return try {
            paths
                .filter { Files.isRegularFile(it) }
                .filter { it.toString().endsWith(".java") || it.toString().endsWith(".kt") }
                .toList()
                .flatMap { path ->
                    Files.readAllLines(path)
                        .asSequence()
                        .filter { it.trimStart().startsWith("import com.beat.support.security.") }
                        .map { line ->
                            line.trim()
                                .removePrefix("import ")
                                .removeSuffix(";")
                                .substringBefore(" as ")
                        }
                        .filter { gatewayImport ->
                            gatewayImport.contains(".internal.") || gatewayImport !in allowedImports
                        }
                        .map { gatewayImport -> "${path}: $gatewayImport" }
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
