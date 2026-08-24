package com.beat.domain.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.FunSpec
import java.nio.file.Files
import java.nio.file.Path

class DomainServiceArchitectureTest : FunSpec({
    val productionClasses: JavaClasses by lazy {
        val productionClassPaths = listOf(
            Path.of("build/classes/kotlin/main"),
            Path.of("build/classes/java/main"),
        ).filter(Files::exists)
        require(productionClassPaths.isNotEmpty()) {
            "Domain production class output is missing"
        }
        ClassFileImporter().importPaths(productionClassPaths)
    }

    test("DomainService는 repository에 의존하지 않는다") {
        noClasses()
            .that()
            .haveSimpleNameEndingWith("DomainService")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..repository..")
            .because("Pure Domain Service: repository and I/O orchestration belong to ApplicationService")
            .check(productionClasses)
    }
})
