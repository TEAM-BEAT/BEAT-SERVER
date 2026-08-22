package com.beat.admin

import com.beat.application.admin.promotion.exception.PromotionApplicationErrorCode
import com.beat.application.admin.user.exception.UserApplicationErrorCode
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class AdminArchitectureGuardTest {

    private val productionClasses: JavaClasses by lazy {
        val productionClassPaths = listOf(
            Path.of("build/classes/kotlin/main"),
            Path.of("build/classes/java/main"),
        ).filter(Files::exists)
        require(productionClassPaths.isNotEmpty()) {
            "Admin production class output is missing"
        }
        ClassFileImporter().importPaths(productionClassPaths)
    }

    @Test
    fun `production classes stay in the admin adapter owner`() {
        classes()
            .should()
            .resideInAnyPackage("com.beat.admin..")
            .because("Admin production classes must remain owned by the admin adapter")
            .check(productionClasses)

        classes()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .resideInAnyPackage("com.beat.admin..api..")
            .because("HTTP controllers belong to admin API packages")
            .check(productionClasses)

        classes()
            .that()
            .haveSimpleNameEndingWith("Facade")
            .should()
            .resideInAnyPackage("com.beat.admin..facade..")
            .because("Admin facades belong to admin adapter packages")
            .check(productionClasses)
    }

    @Test
    fun `legacy owner packages are absent from the compiled admin output`() {
        val violations = productionClasses
            .filter { it.packageName.startsWith("com.beat.domain.") || it.packageName.startsWith("com.beat.global.") }
            .map(JavaClass::getFullName)
            .sorted()

        assertTrue(
            violations.isEmpty(),
            "Admin production output must not reintroduce legacy owner packages: ${violations.joinToString(", ")}",
        )
    }

    @Test
    fun `production classes do not depend on domain types`() {
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.beat.domain..")
            .because("Admin production code must not depend on the Domain module")
            .check(productionClasses)
    }

    @Test
    fun `dto adapters do not depend on domain types`() {
        noClasses()
            .that()
            .resideInAnyPackage(
                "com.beat.admin..api.request..",
                "com.beat.admin..api.response..",
                "com.beat.admin..application.result..",
            )
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.beat.domain..")
            .because("Admin transport contracts must not expose Domain types")
            .check(productionClasses)
    }

    @Test
    fun `controllers do not depend on concrete application services or infrastructure`() {
        noClasses()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat(concreteApplicationServiceOrInfrastructure)
            .because("Admin controllers must enter use cases through facades")
            .check(productionClasses)
    }

    @Test
    fun `facades do not depend on domain or infrastructure implementations`() {
        noClasses()
            .that()
            .haveSimpleNameEndingWith("Facade")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.beat.domain..", "com.beat.infra..")
            .because("Admin facades must delegate to application services")
            .check(productionClasses)
    }

    @Test
    fun `only admin configuration and bootstrap may depend on public infrastructure configuration`() {
        noClasses()
            .that(nonBootstrapClasses)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.beat.infra..")
            .because("Infrastructure configuration is visible only to admin bootstrap configuration")
            .check(productionClasses)
    }

    @Test
    fun `admin classes do not reach gateway internals or legacy runtime lanes`() {
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.beat.support.security..internal..",
                "com.beat.batch..",
                "com.beat.global..",
                "com.beat.legacyroot..",
            )
            .because("Admin adapters use public support boundaries and their assigned runtime lane")
            .check(productionClasses)
    }

    @Test
    fun `transitional admin packages and mapper ownership are absent from compiled output`() {
        val transitionalPackages = listOf(
            "com.beat.admin.adapter",
            "com.beat.admin.controller",
            "com.beat.admin.port.in",
            "com.beat.admin.application.service",
        )
        val transitionalViolations = productionClasses
            .filter { javaClass -> transitionalPackages.any { packageName ->
                javaClass.packageName == packageName || javaClass.packageName.startsWith("$packageName.")
            } }
            .map(JavaClass::getFullName)

        val mapperViolations = productionClasses
            .filter { it.simpleName.endsWith("Mapper") }
            .map(JavaClass::getFullName)

        assertTrue(
            transitionalViolations.isEmpty() && mapperViolations.isEmpty(),
            "Transitional admin classes or mapper implementations remain: " +
                (transitionalViolations + mapperViolations).joinToString(", "),
        )
    }

    @Test
    fun `admin application error codes remain unique`() {
        val codes = PromotionApplicationErrorCode.entries.map { it.code } +
            UserApplicationErrorCode.entries.map { it.code }

        assertEquals(codes.size, codes.distinct().size)
    }

    private val concreteApplicationServiceOrInfrastructure =
        object : DescribedPredicate<JavaClass>("be a concrete application service or infrastructure type") {
            override fun test(input: JavaClass): Boolean {
                return input.packageName.startsWith("com.beat.infra.") ||
                    (input.packageName.startsWith("com.beat.application.") && input.simpleName.endsWith("Service"))
            }
        }

    private val nonBootstrapClasses =
        object : DescribedPredicate<JavaClass>("be an admin class outside configuration/bootstrap") {
            override fun test(input: JavaClass): Boolean {
                if (!input.packageName.startsWith("com.beat.admin.")) {
                    return false
                }
                return !input.packageName.contains(".config") && input.simpleName != "AdminApplication"
            }
        }
}
