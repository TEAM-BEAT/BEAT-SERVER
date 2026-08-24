package com.beat.apps.api

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.FunSpec
import java.nio.file.Files
import java.nio.file.Path

/**
 * API composition-root contracts that neither the Gradle module graph nor Kotlin
 * visibility can enforce. Dependency-direction rules live in verifyTargetModuleGraph;
 * infrastructure implementation hiding is enforced by `internal`. Do not duplicate them here.
 */
class ApisArchitectureGuardTest : FunSpec({
    val productionClasses: JavaClasses by lazy {
        val productionClassPaths = listOf(
            Path.of("build/classes/kotlin/main"),
            Path.of("build/classes/java/main"),
        ).filter(Files::exists)
        require(productionClassPaths.isNotEmpty()) {
            "API production class output is missing"
        }
        ClassFileImporter().importPaths(productionClassPaths)
    }

    val concreteApplicationServiceOrInfrastructure =
        object : DescribedPredicate<JavaClass>("be a concrete application service or infrastructure type") {
            override fun test(input: JavaClass): Boolean =
                input.packageName.startsWith("com.beat.infrastructure.") ||
                    (input.packageName.startsWith("com.beat.application.") && input.simpleName.endsWith("Service"))
        }

    test("controller와 facade는 각자의 어댑터 패키지에 위치한다") {
        classes()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .resideInAnyPackage("com.beat.apps.api..api..")
            .because("HTTP controllers belong to API adapter packages")
            .check(productionClasses)

        classes()
            .that()
            .haveSimpleNameEndingWith("Facade")
            .should()
            .resideInAnyPackage("com.beat.apps.api..facade..")
            .because("API facades belong to API adapter packages")
            .check(productionClasses)
    }

    test("controller는 api facade를 통해서만 유즈케이스에 진입한다") {
        noClasses()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat(concreteApplicationServiceOrInfrastructure)
            .because("Controllers must enter use cases through API facades, never call ApplicationService or infrastructure directly")
            .check(productionClasses)
    }

    test("infra 공개 설정 타입은 bootstrap만 사용할 수 있다") {
        val nonBootstrapClasses = object : DescribedPredicate<JavaClass>(
            "be an API class outside configuration/bootstrap",
        ) {
            override fun test(input: JavaClass): Boolean {
                if (!input.packageName.startsWith("com.beat.apps.api.")) {
                    return false
                }
                return !input.packageName.contains(".config") && input.simpleName != "ApisApplication"
            }
        }
        noClasses()
            .that(nonBootstrapClasses)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.beat.infrastructure..")
            .because("Infrastructure wiring types may only be consumed by API bootstrap configuration")
            .check(productionClasses)
    }
})
