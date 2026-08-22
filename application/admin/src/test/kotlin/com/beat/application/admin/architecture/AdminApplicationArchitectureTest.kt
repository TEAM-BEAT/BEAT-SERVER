package com.beat.application.admin.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.Test
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path

class AdminApplicationArchitectureTest {
    private val productionClasses: JavaClasses by lazy {
        val productionClassPaths = listOf(
            Path.of("build/classes/kotlin/main"),
            Path.of("build/classes/java/main"),
        ).filter(Files::exists)
        require(productionClassPaths.isNotEmpty()) {
            "Admin application production class output is missing"
        }
        ClassFileImporter().importPaths(productionClassPaths)
    }

    @Test
    fun `all admin services depend on domain failure translator`() {
        classes()
            .that()
            .areAnnotatedWith(Service::class.java)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName(
                "com.beat.application.admin.exception.DomainFailureTranslatorKt",
            )
            .because("Admin services must translate domain failures at their application boundary")
            .check(productionClasses)
    }
}
