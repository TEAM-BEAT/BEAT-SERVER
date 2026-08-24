package com.beat.application.system.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.FunSpec
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path

class SystemApplicationArchitectureTest : FunSpec({
    val productionClasses: JavaClasses by lazy {
        val productionClassPaths = listOf(
            Path.of("build/classes/kotlin/main"),
            Path.of("build/classes/java/main"),
        ).filter(Files::exists)
        require(productionClassPaths.isNotEmpty()) {
            "System application production class output is missing"
        }
        ClassFileImporter().importPaths(productionClassPaths)
    }

    test("ApplicationService는 다른 ApplicationService에 직접 의존하지 않는다") {
        noClasses()
            .that()
            .areAnnotatedWith(Service::class.java)
            .should()
            .dependOnClassesThat()
            .areAnnotatedWith(Service::class.java)
            .because("No Application Service Graph: use-case entry points must not form a concrete service graph")
            .check(productionClasses)
    }
})
