package com.beat.application.frontoffice.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class FrontofficeApplicationArchitectureTest {
    private val importedClasses: JavaClasses =
        ClassFileImporter().importPackages("com.beat.application.frontoffice")

    @Test
    fun `command packages must not depend on query packages`() {
        checkRule(
            noDependencyRule(packagePattern("command"), packagePattern("query")),
        )
    }

    @Test
    fun `booking packages must not depend on performance lanes`() {
        checkRule(
            noDependencyRule(
                packagePattern("booking"),
                packagePattern("performance", "booker"),
                packagePattern("performance", "maker"),
            ),
        )
    }

    @Test
    fun `booker query must not depend on maker`() {
        checkRule(
            noDependencyRule(
                packagePattern("performance", "booker", "query"),
                packagePattern("performance", "maker"),
            ),
        )
    }

    @Test
    fun `maker query must not depend on booker or maker command`() {
        checkRule(
            noDependencyRule(
                packagePattern("performance", "maker", "query"),
                packagePattern("performance", "booker"),
                packagePattern("performance", "maker", "command"),
            ),
        )
    }

    @Test
    fun `maker command must not depend on booker maker query or schedule query`() {
        checkRule(
            noDependencyRule(
                packagePattern("performance", "maker", "command"),
                packagePattern("performance", "booker"),
                packagePattern("performance", "maker", "query"),
                packagePattern("schedule", "query"),
            ),
        )
    }

    @Test
    fun `ticket application must not depend on adapters contracts infrastructure web or global support`() {
        checkRule(
            noDependencyRule(
                packagePattern("ticket"),
                packagePattern("apis"),
                packagePattern("admin"),
                packagePattern("batch"),
                packagePattern("contracts"),
                packagePattern("infra"),
                packagePattern("web"),
                packagePattern("global"),
            ),
        )
    }

    @Test
    fun `ticket application must not depend on performance maker services`() {
        checkRule(
            noDependencyRule(
                packagePattern("ticket"),
                packagePattern("performance", "maker", "command"),
                packagePattern("performance", "maker", "query"),
                packagePattern("performance", "application"),
            ),
        )
    }

    private fun checkRule(rule: ArchRule?) {
        rule?.check(importedClasses)
    }

    private fun noDependencyRule(
        sourcePackage: PackagePattern,
        vararg targetPackages: PackagePattern,
    ): ArchRule? {
        if (!hasPackage(sourcePackage)) {
            return null
        }
        val targets = targetPackages.map { it.archUnitPattern }.toTypedArray()
        return noClasses()
            .that()
            .resideInAnyPackage(sourcePackage.archUnitPattern)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*targets)
            .because(
                "${sourcePackage.archUnitPattern} must not depend on ${targets.joinToString()}",
            )
    }

    private fun hasPackage(packagePattern: PackagePattern): Boolean {
        return importedClasses.any { javaClass ->
            javaClass.packageName
                .split('.')
                .windowed(packagePattern.segments.size)
                .any { it == packagePattern.segments }
        }
    }

    private fun packagePattern(vararg segments: String): PackagePattern {
        return PackagePattern(
            archUnitPattern = "..${segments.joinToString(".")}..",
            segments = segments.toList(),
        )
    }

    private data class PackagePattern(
        val archUnitPattern: String,
        val segments: List<String>,
    )
}
