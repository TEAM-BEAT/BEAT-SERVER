package com.beat.application.system.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.FunSpec
import java.nio.file.Files
import java.nio.file.Path
import org.springframework.stereotype.Service

class SystemApplicationArchitectureTest :
    FunSpec({
        val productionClasses: JavaClasses by lazy {
            val productionClassPaths =
                listOf(
                        Path.of("build/classes/kotlin/main"),
                        Path.of("build/classes/java/main"),
                    )
                    .filter(Files::exists)
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
                .because(
                    "No Application Service Graph: use-case entry points must not form a concrete service graph"
                )
                .check(productionClasses)
        }

        test("system Application은 기술 구현에 의존하지 않는다") {
            noClasses()
                .that()
                .resideInAnyPackage("com.beat.application.system", "com.beat.application.system..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                    "jakarta.persistence..",
                    "org.springframework.web..",
                    "org.springframework.data.redis..",
                    "org.redisson..",
                    "com.linecorp.kotlinjdsl..",
                    "org.jooq..",
                    "com.beat.infrastructure.jooq.generated..",
                )
                .because("System Application technology boundary — jOOQ containment (E-08)")
                .check(productionClasses)
        }

        test("system Application은 infrastructure jOOQ generated 타입에 의존하지 않는다") {
            noClasses()
                .that()
                .resideInAnyPackage("com.beat.application.system", "com.beat.application.system..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.beat.infrastructure.jooq.generated..")
                .because("jOOQ type leakage = 0 (I-21)")
                .check(productionClasses)
        }
    })
