package com.beat.apps.admin

import com.beat.application.admin.promotion.exception.PromotionApplicationErrorCode
import com.beat.application.admin.user.exception.UserApplicationErrorCode
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path

/**
 * Admin composition-root contracts. Module-level isolation is enforced by the Gradle
 * graph and Kotlin `internal`; these rules cover composition-root discipline only.
 */
class AdminArchitectureGuardTest : FunSpec({
    val productionClasses: JavaClasses by lazy {
        val productionClassPaths = listOf(
            Path.of("build/classes/kotlin/main"),
            Path.of("build/classes/java/main"),
        ).filter(Files::exists)
        require(productionClassPaths.isNotEmpty()) {
            "Admin production class output is missing"
        }
        ClassFileImporter().importPaths(productionClassPaths)
    }

    val infrastructureType =
        object : DescribedPredicate<JavaClass>("be an infrastructure type") {
            override fun test(input: JavaClass): Boolean =
                input.packageName.startsWith("com.beat.infrastructure.")
        }

    test("controller와 facade는 각자의 어댑터 패키지에 위치한다") {
        classes()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .resideInAnyPackage("com.beat.apps.admin..api..")
            .because("HTTP controllers belong to admin API packages")
            .check(productionClasses)

        classes()
            .that()
            .haveSimpleNameEndingWith("Facade")
            .should()
            .resideInAnyPackage("com.beat.apps.admin..facade..")
            .because("Admin facades belong to admin adapter packages")
            .check(productionClasses)
    }

    test("controller는 public Application API 또는 adapter-local Facade를 통해서만 유즈케이스에 진입한다") {
        noClasses()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat(infrastructureType)
            .because("Controllers must not depend on infrastructure directly; Facade is standard but direct public Application API is allowed")
            .check(productionClasses)
    }

    test("infra 공개 설정 타입은 bootstrap만 사용할 수 있다") {
        val nonBootstrapClasses = object : DescribedPredicate<JavaClass>(
            "be an admin class outside configuration/bootstrap",
        ) {
            override fun test(input: JavaClass): Boolean {
                if (!input.packageName.startsWith("com.beat.apps.admin.")) {
                    return false
                }
                return !input.packageName.contains(".config") && input.simpleName != "AdminApplication"
            }
        }
        noClasses()
            .that(nonBootstrapClasses)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.beat.infrastructure..")
            .because("Infrastructure wiring types may only be consumed by admin bootstrap configuration")
            .check(productionClasses)
    }

    test("admin application 에러 코드는 전체에서 고유하다") {
        val codes = PromotionApplicationErrorCode.entries.map { it.code } +
            UserApplicationErrorCode.entries.map { it.code }

        codes.size shouldBe codes.distinct().size
    }
})
