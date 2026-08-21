package com.beat.application.frontoffice.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
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
    fun `command packages must not depend on presentation read models`() {
        checkRule(noCommandDependencyOnPresentationReadModelsRule())
    }

    @Test
    fun `booking booker packages must not depend on performance lanes`() {
        checkRule(
            noDependencyRule(
                packagePattern("booking", "booker"),
                packagePattern("performance", "booker"),
                packagePattern("performance", "maker"),
            ),
        )
    }

    @Test
    fun `booking classes must reside in the booker lane`() {
        checkActorAlignment("booking", "booking.booker")
    }

    @Test
    fun `ticket classes must reside in the maker lane`() {
        checkActorAlignment("ticket", "ticket.maker")
    }

    @Test
    fun `legacy schedule query package must not exist`() {
        val legacyPackage = "com.beat.application.frontoffice.schedule.query"
        val violations = importedClasses
            .filter { javaClass ->
                javaClass.packageName == legacyPackage ||
                    javaClass.packageName.startsWith("$legacyPackage.")
            }
            .map(JavaClass::getFullName)
            .sorted()

        check(violations.isEmpty()) {
            "Legacy schedule.query classes must not exist: ${violations.joinToString(", ")}"
        }
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
                packagePattern("schedule", "booker", "query"),
            ),
        )
    }

    @Test
    fun `ticket maker application must not depend on adapters contracts infrastructure web or global support`() {
        checkRule(
            noDependencyRule(
                packagePattern("ticket", "maker"),
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
    fun `ticket maker application must not depend on performance maker services`() {
        checkRule(
            noDependencyRule(
                packagePattern("ticket", "maker"),
                packagePattern("performance", "maker", "command"),
                packagePattern("performance", "maker", "query"),
                packagePattern("performance", "application"),
            ),
        )
    }

    @Test
    fun `member and auth lanes must not depend on runtime adapters or state frameworks`() {
        val forbiddenPackages = arrayOf(
            packagePattern("apps"),
            packagePattern("admin"),
            packagePattern("batch"),
            packagePattern("contracts"),
            packagePattern("infra"),
            packagePattern("global"),
            packagePattern("web"),
            packagePattern("jakarta", "persistence"),
            packagePattern("jakarta", "servlet"),
            packagePattern("org", "springframework", "data", "redis"),
            packagePattern("org", "springframework", "http"),
            packagePattern("org", "springframework", "web"),
        )
        checkRule(noDependencyRule(packagePattern("member"), *forbiddenPackages))
        checkRule(noDependencyRule(packagePattern("auth"), *forbiddenPackages))
    }

    @Test
    fun `auth must not depend on member`() {
        checkRule(
            noDependencyRule(
                packagePattern("auth"),
                packagePattern("member"),
            ),
        )
    }

    @Test
    fun `member may depend on only the auth login session boundary`() {
        checkRule(noMemberDependencyOnAuthTypesRule())
    }

    @Test
    fun `member must not depend directly on support token issuers`() {
        checkRule(
            noDependencyOnConcreteTypesRule(
                packagePattern("member"),
                setOf(
                    "com.beat.support.security.token.TokenIssuer",
                    "com.beat.support.security.token.RefreshTokenAuthenticator",
                ),
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

    private fun checkActorAlignment(sourceSegment: String, actorPackage: String) {
        val sourcePrefix = "com.beat.application.frontoffice.$sourceSegment"
        val actorPrefix = "com.beat.application.frontoffice.$actorPackage"
        val violations = importedClasses
            .filter { javaClass ->
                val packageName = javaClass.packageName
                (packageName == sourcePrefix || packageName.startsWith("$sourcePrefix.")) &&
                    packageName != actorPrefix && !packageName.startsWith("$actorPrefix.")
            }
            .map(JavaClass::getFullName)
            .sorted()

        check(violations.isEmpty()) {
            "$sourcePrefix classes must reside under $actorPrefix: ${violations.joinToString(", ")}"
        }
    }

    private fun noCommandDependencyOnPresentationReadModelsRule(): ArchRule? {
        val sourcePackage = packagePattern("command")
        if (!hasPackage(sourcePackage)) {
            return null
        }
        val presentationReadModel = object : DescribedPredicate<JavaClass>(
            "be module-contracts read models or classes annotated with @ReadModel",
        ) {
            override fun test(input: JavaClass): Boolean =
                input.packageName.split('.').contains("readmodel") ||
                    input.isAnnotatedWith("com.beat.contracts.common.ReadModel")
        }
        return noClasses()
            .that()
            .resideInAnyPackage(sourcePackage.archUnitPattern)
            .should()
            .dependOnClassesThat(presentationReadModel)
            .because(
                "${sourcePackage.archUnitPattern} correctness code must not depend on module-contracts presentation projections",
            )
    }

    private fun noMemberDependencyOnAuthTypesRule(): ArchRule? {
        val allowedAuthTypes = setOf(
            "com.beat.application.frontoffice.auth.command.LoginSessionIssuer",
            "com.beat.application.frontoffice.auth.command.LoginSession",
        )
        val forbiddenAuthTypes = importedClasses
            .filter { javaClass ->
                javaClass.packageName.startsWith("com.beat.application.frontoffice.auth") &&
                    javaClass.fullName !in allowedAuthTypes
            }
            .map(JavaClass::getFullName)
            .toSet()
        return noDependencyOnConcreteTypesRule(packagePattern("member"), forbiddenAuthTypes)
    }

    private fun noDependencyOnConcreteTypesRule(
        sourcePackage: PackagePattern,
        forbiddenTypes: Set<String>,
    ): ArchRule? {
        if (!hasPackage(sourcePackage) || forbiddenTypes.isEmpty()) {
            return null
        }
        val forbiddenTypePredicate = object : DescribedPredicate<JavaClass>(
            "have forbidden concrete types ${forbiddenTypes.sorted().joinToString()}",
        ) {
            override fun test(input: JavaClass): Boolean = input.fullName in forbiddenTypes
        }
        return noClasses()
            .that()
            .resideInAnyPackage(sourcePackage.archUnitPattern)
            .should()
            .dependOnClassesThat(forbiddenTypePredicate)
            .because(
                "${sourcePackage.archUnitPattern} must not depend on forbidden concrete types "
                    + forbiddenTypes.sorted().joinToString(),
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
