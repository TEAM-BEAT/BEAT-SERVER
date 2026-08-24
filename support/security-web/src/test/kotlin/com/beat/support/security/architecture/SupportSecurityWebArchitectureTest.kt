package com.beat.support.security.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.FunSpec
import java.nio.file.Files
import java.nio.file.Path

class SupportSecurityWebArchitectureTest : FunSpec({
    val productionClasses: JavaClasses by lazy {
        val productionClassPaths = listOf(
            Path.of("build/classes/kotlin/main"),
            Path.of("build/classes/java/main"),
        ).filter(Files::exists)
        require(productionClassPaths.isNotEmpty()) {
            "Support security-web production class output is missing"
        }
        ClassFileImporter().importPaths(productionClassPaths)
    }

    test("security-web은 application/domain/infrastructure/apps를 직접 참조하지 않는다") {
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.beat.application..",
                "com.beat.domain..",
                "com.beat.infrastructure..",
                "com.beat.apps..",
            )
            .because("security-web may depend only on support:security and support:observability")
            .check(productionClasses)
    }
})
