package com.beat.apis

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class ApisArchitectureGuardTest {

    private val productionClasses: JavaClasses by lazy {
        val productionClassPaths = listOf(
            Path.of("build/classes/kotlin/main"),
            Path.of("build/classes/java/main"),
        ).filter(Files::exists)
        require(productionClassPaths.isNotEmpty()) {
            "API production class output is missing"
        }
        ClassFileImporter().importPaths(productionClassPaths)
    }

    @Test
    fun `production classes stay in the api adapter owner`() {
        classes()
            .should()
            .resideInAnyPackage("com.beat.apis..")
            .because("API production classes must remain owned by the API adapter")
            .check(productionClasses)

        classes()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .resideInAnyPackage("com.beat.apis..api..")
            .because("HTTP controllers belong to API adapter packages")
            .check(productionClasses)

        classes()
            .that()
            .haveSimpleNameEndingWith("Facade")
            .should()
            .resideInAnyPackage("com.beat.apis..facade..")
            .because("API facades belong to API adapter packages")
            .check(productionClasses)
    }

    @Test
    fun `legacy owner packages are absent from the compiled api output`() {
        val violations = productionClasses
            .filter { it.packageName.startsWith("com.beat.domain.") || it.packageName.startsWith("com.beat.global.") }
            .map(JavaClass::getFullName)
            .sorted()

        assertTrue(
            violations.isEmpty(),
            "API production output must not reintroduce legacy owner packages: ${violations.joinToString(", ")}",
        )
    }

    @Test
    fun `production classes do not depend on domain types`() {
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.beat.domain..")
            .because("API production code must depend on application contracts, not Domain types")
            .check(productionClasses)
    }

    @Test
    fun `dto and event adapters do not depend on domain types`() {
        noClasses()
            .that()
            .resideInAnyPackage(
                "com.beat.apis..api.request..",
                "com.beat.apis..api.response..",
                "com.beat.apis..application.result..",
                "com.beat.apis..application.event..",
            )
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.beat.domain..")
            .because("API transport and event contracts must not expose Domain types")
            .check(productionClasses)
    }

    @Test
    fun `controllers do not depend on concrete application services or infrastructure`() {
        noClasses()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat(concreteApplicationServiceOrInfrastructure)
            .because("Controllers must enter use cases through API facades")
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
            .because("Facades must delegate to application services")
            .check(productionClasses)
    }

    @Test
    fun `only api configuration and bootstrap may depend on public infrastructure configuration`() {
        noClasses()
            .that(nonBootstrapClasses)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.beat.infra..")
            .because("Infrastructure configuration is visible only to API bootstrap configuration")
            .check(productionClasses)
    }

    @Test
    fun `api classes do not reach gateway internals or legacy runtime lanes`() {
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.beat.support.security..internal..",
                "com.beat.batch..",
                "com.beat.global..",
                "com.beat.legacyroot..",
            )
            .because("API adapters use public support boundaries and their assigned runtime lane")
            .check(productionClasses)
    }

    @Test
    fun `provider specific api packages are absent`() {
        classes()
            .should()
            .resideOutsideOfPackages(
                "com.beat.apis.external.s3..",
                "com.beat.apis.external.sms..",
                "com.beat.apis.external.image..",
                "com.beat.apis.external.notification.slack..",
            )
            .because("Provider-specific adapters belong to infrastructure")
            .check(productionClasses)
    }

    private val concreteApplicationServiceOrInfrastructure =
        object : DescribedPredicate<JavaClass>("be a concrete application service or infrastructure type") {
            override fun test(input: JavaClass): Boolean {
                return input.packageName.startsWith("com.beat.infra.") ||
                    (input.packageName.startsWith("com.beat.application.") && input.simpleName.endsWith("Service"))
            }
        }

    private val nonBootstrapClasses =
        object : DescribedPredicate<JavaClass>("be an API class outside configuration/bootstrap") {
            override fun test(input: JavaClass): Boolean {
                if (!input.packageName.startsWith("com.beat.apis.")) {
                    return false
                }
                return !input.packageName.contains(".config") && input.simpleName != "ApisApplication"
            }
        }
}
