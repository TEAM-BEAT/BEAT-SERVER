package com.beat.application.admin.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.FunSpec
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path

class AdminApplicationArchitectureTest : FunSpec({
    val productionClasses: JavaClasses by lazy {
        val productionClassPaths = listOf(
            Path.of("build/classes/kotlin/main"),
            Path.of("build/classes/java/main"),
        ).filter(Files::exists)
        require(productionClassPaths.isNotEmpty()) {
            "Admin application production class output is missing"
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

    test("admin Application은 기술 구현에 의존하지 않는다") {
        noClasses()
            .that()
            .resideInAnyPackage("com.beat.application.admin", "com.beat.application.admin..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "jakarta.persistence..",
                "org.springframework.web..",
                "org.springframework.data.redis..",
                "org.redisson..",
                "com.linecorp.kotlinjdsl..",
            )
            .because("Admin Application technology boundary")
            .check(productionClasses)
    }
})
