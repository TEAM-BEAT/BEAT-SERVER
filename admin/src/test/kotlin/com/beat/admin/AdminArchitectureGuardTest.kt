package com.beat.admin

import com.beat.admin.promotion.exception.PromotionApplicationErrorCode
import com.beat.admin.user.exception.UserApplicationErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
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
            "com.beat.global.common.config.",
            "com.beat.global.support.config.",
        )

        assertTrue(violations.isEmpty(), "Found forbidden root bootstrap references:\n${violations.joinToString("\n")}")
    }

    @Test
    fun `admin sources import only public gateway boundary`() {
        val violations = findGatewayImportViolations(
            setOf(
                "com.beat.support.security.EnableGatewayConfig",
                "com.beat.support.security.GatewayConfigGroup",
                "com.beat.support.security.CurrentMember",
                "com.beat.support.security.EnableGatewayServletSecurity",
            )
        )

        assertTrue(
            violations.isEmpty(),
            "Found non-public gateway imports:\n${violations.joinToString("\n")}"
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
            Path.of("src/main"),
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
    fun `admin response and application codes stay in their owning boundaries`() {
        assertTrue(Files.exists(Path.of("src/main/kotlin/com/beat/admin/promotion/api/response/PromotionSuccessCode.kt")))
        assertTrue(Files.exists(Path.of("src/main/kotlin/com/beat/admin/user/api/response/UserSuccessCode.kt")))
        assertTrue(Files.exists(Path.of("src/main/kotlin/com/beat/admin/promotion/exception/PromotionApplicationErrorCode.kt")))
        assertTrue(Files.exists(Path.of("src/main/kotlin/com/beat/admin/user/exception/UserApplicationErrorCode.kt")))
        assertFalse(Files.exists(Path.of("src/main/kotlin/com/beat/admin/api/response/AdminSuccessCode.kt")))
        assertFalse(Files.exists(Path.of("src/main/kotlin/com/beat/admin/application/exception/AdminApplicationErrorCode.kt")))
        assertFalse(Files.exists(Path.of("src/main/kotlin/com/beat/admin/exception/AdminMemberApplicationErrorCode.kt")))
        assertFalse(Files.exists(Path.of("src/main/java/com/beat/admin/exception/AdminSuccessCode.java")))
    }

    @Test
    fun `admin does not declare mapper packages`() {
        val violations = findSourceFilesInMatchingPaths(Path.of("src/main"), "/mapper/")

        assertTrue(
            violations.isEmpty(),
            "Found mapper files in admin module:\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `admin application error codes are unique`() {
        val codes = PromotionApplicationErrorCode.entries.map { it.getCode() } +
            UserApplicationErrorCode.entries.map { it.getCode() }

        assertEquals(codes.size, codes.distinct().size)
    }

    @Test
    fun `admin dto layer must not import domain types`() {
        val violations = listOf("/api/request/", "/api/response/", "/application/result/")
            .flatMap { pathSegment ->
                findForbiddenImportsInMatchingPaths(
                    Path.of("src/main"),
                    pathSegment = pathSegment,
                    "com.beat.domain.",
                )
            }

        assertTrue(
            violations.isEmpty(),
            "Found domain type imports in admin DTO layer:\n${violations.joinToString("\n")}"
        )
    }

    @Test
    fun `admin application must not depend on http dto`() {
        val violations = findForbiddenImportsInMatchingPaths(
            Path.of("src/main"),
            pathSegment = "/application/",
            ".api.request.",
            ".api.response.",
        )

        assertTrue(violations.isEmpty(), "Admin application must not import HTTP DTOs:\n${violations.joinToString("\n")}")
    }

    @Test
    fun `admin controllers must enter use cases through facades`() {
        val violations = findControllerForbiddenImports(
            Path.of("src/main"),
            ".application.",
            "com.beat.domain.",
            "com.beat.contracts.",
            "com.beat.infra.",
        )

        assertTrue(violations.isEmpty(), "Admin controllers must depend on facades only:\n${violations.joinToString("\n")}")
    }

    @Test
    fun `admin application services do not return raw domain models`() {
        val violations = findMethodSignatureViolations(
            Path.of("src/main"),
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

    private fun findSourceFilesInMatchingPaths(root: Path, pathSegment: String): List<String> {
        val paths = Files.walk(root)

        return try {
            paths
                .filter(Files::isRegularFile)
                .filter { path -> path.toString().contains(pathSegment) }
                .filter { path -> path.toString().endsWith(".java") || path.toString().endsWith(".kt") }
                .map(Path::toString)
                .toList()
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

    private fun findControllerForbiddenImports(root: Path, vararg forbiddenReferences: String): List<String> {
        val paths = Files.walk(root)

        return try {
            paths
                .filter(Files::isRegularFile)
                .filter { path -> path.fileName.toString().matches(Regex(""".*Controller\.(java|kt)""")) }
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

    private fun findGatewayImportViolations(allowedImports: Set<String>): List<String> {
        val paths = Files.walk(Path.of("src/main"))

        return try {
            paths
                .filter(Files::isRegularFile)
                .filter { path -> path.toString().endsWith(".java") || path.toString().endsWith(".kt") }
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
                        .map { gatewayImport -> "$path: $gatewayImport" }
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
