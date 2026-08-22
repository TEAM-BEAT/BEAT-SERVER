package com.beat

import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class BuildToolingContractSpec : FunSpec() {
    private val checkerTempRoot = tempdir().toPath()

    init {
            test("observabilityBuildKeepsSentryRuntimeOnlyAndAvoidsSharedBoundaryLeaks") {
            val observabilityBuild = read("observability/build.gradle.kts")
            val uncommented = stripLineComments(observabilityBuild)

            (uncommented.contains("compileOnly(libs.spring.boot.starter.web)")) shouldBe true
            (uncommented.contains("implementation(libs.kotlinx.coroutines.slf4j)")) shouldBe true
            (uncommented.contains("implementation(libs.sentry.spring.boot.starter)")) shouldBe true
            (uncommented.contains("runtimeOnly(libs.sentry.async.profiler)")) shouldBe true
            (uncommented.contains("runtimeOnly(libs.sentry.log4j2)")) shouldBe true

            (uncommented.contains("project(\":global-support\")")) shouldBe false
            (uncommented.contains("libs.lombok")) shouldBe false
            (uncommented.contains("annotationProcessor")) shouldBe false
            (uncommented.contains("libs.spring.boot.starter.actuator")) shouldBe false
            (uncommented.contains("libs.slf4j.api")) shouldBe false
            (uncommented.contains("slf4j-api")) shouldBe false
        }


            test("staleDependencyBoundaryCatalogAliasesDoNotReturn") {
            val catalog = read("gradle/libs.versions.toml")

            assertCatalogAliasAbsent(catalog, "versions", "awspring")
            assertCatalogAliasAbsent(catalog, "versions", "querydsl")
            assertCatalogAliasAbsent(catalog, "versions", "slf4j")
            assertCatalogAliasAbsent(catalog, "plugins", "spring-boot")
            assertCatalogAliasAbsent(catalog, "plugins", "spring-dependency-management")
            assertCatalogAliasAbsent(catalog, "plugins", "kotlin-jvm")
            assertCatalogAliasAbsent(catalog, "plugins", "kotlin-spring")
            assertCatalogAliasAbsent(catalog, "plugins", "kotlin-jpa")
            assertCatalogAliasAbsent(catalog, "plugins", "sentry-jvm")
            assertCatalogAliasAbsent(catalog, "libraries", "awspring-cloud-aws-starter-s3")
            assertCatalogAliasAbsent(catalog, "libraries", "querydsl-jpa")
            assertCatalogAliasAbsent(catalog, "libraries", "querydsl-apt")
            assertCatalogAliasAbsent(catalog, "libraries", "spring-security-core")
            assertCatalogAliasAbsent(catalog, "libraries", "slf4j-api")
            assertCatalogAliasAbsent(catalog, "bundles", "test-common")
            assertCatalogAliasAbsent(catalog, "bundles", "web-app-god")
        }


            test("versionCatalogCheckerDoesNotTreatCommentedGradleAccessorsAsUsage") {
            val tempRoot = checkerTempRoot
            val gradleDir = tempRoot.resolve("gradle")
            Files.createDirectories(gradleDir)
            Files.writeString(gradleDir.resolve("libs.versions.toml"), """
                [versions]
                used = "1.0.0"
                lookup = "1.0.0"
                unused = "1.0.0"

                [libraries]
                used-lib = { module = "com.example:used", version.ref = "used" }
                lookup-lib = { module = "com.example:lookup", version.ref = "lookup" }
                unused-lib = { module = "com.example:unused", version.ref = "unused" }
                """.trimIndent())
            Files.writeString(tempRoot.resolve("build.gradle.kts"), listOf(
                "// implementation(libs.unused.lib)",
                "val stringMention = \"libs.unused.lib\"",
                "val multilineMention = \"\"\"",
                "    libs.unused.lib",
                "\"\"\"",
                "dependencies {",
                "    implementation(libs.used.lib)",
                "    implementation(libs.findLibrary(\"lookup-lib\").get())",
                "    /*",
                "     * implementation(libs.unused.lib)",
                "     */",
                "}"
    ).joinToString("\n"))

            val checker = Path.of(".github/scripts/check_unused_version_catalog_aliases.py")
                .toAbsolutePath()
                .normalize()
            val process = ProcessBuilder("python3", checker.toString(), "--root", tempRoot.toString())
                .redirectErrorStream(true)
                .start()
            val output = String(process.inputStream.readAllBytes(), StandardCharsets.UTF_8)
            val exitCode = process.waitFor()

            (exitCode) shouldBe (1)
            (output.contains("libraries.unused-lib")) shouldBe true
            (output.contains("versions.unused")) shouldBe true
        }

    }
}

private fun assertCatalogAliasAbsent(catalog: String, section: String, alias: String) {
    val sectionBody = sectionBody(catalog, section)
    sectionBody.matches(Regex("(?ms).*^${Regex.escape(alias)}\\s*=.*")) shouldBe false
}

private fun sectionBody(catalog: String, section: String): String {
    val marker = "[$section]"
    val start = catalog.indexOf(marker)
    (start >= 0) shouldBe true
    val next = catalog.indexOf("\n[", start + marker.length)
    return if (next < 0) catalog.substring(start) else catalog.substring(start, next)
}

private fun stripLineComments(source: String): String =
    source.replace(Regex("(?m)//.*\$"), "")
