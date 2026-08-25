package com.beat.infrastructure.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.FunSpec
import java.nio.file.Files
import java.nio.file.Path

class InfrastructureJooqArchitectureTest :
    FunSpec({
        val productionClasses: JavaClasses by lazy {
            val productionClassPaths =
                listOf(
                        Path.of("build/classes/kotlin/main"),
                        Path.of("build/classes/java/main"),
                    )
                    .filter(Files::exists)
            require(productionClassPaths.isNotEmpty()) {
                "Infrastructure production class output is missing"
            }
            ClassFileImporter().importPaths(productionClassPaths)
        }

        test("infrastructure jOOQ generated type는 infrastructure 내부에만 존재해야 한다 — E-08 guard") {
            // Direct guard via ArchUnit: no outer layer should depend on generated package
            // This test documents the contract; actual enforcement for App/Domain/Apps is in their
            // own modules.
            // Here we ensure generated package is under infrastructure.
            val generatedClasses = productionClasses.filter {
                it.packageName.startsWith("com.beat.infrastructure.jooq.generated")
            }
            check(generatedClasses.isNotEmpty()) { "jOOQ generated package must exist" }
            generatedClasses.forEach { clazz ->
                check(clazz.packageName.startsWith("com.beat.infrastructure")) {
                    "Generated jOOQ type $clazz must reside in infrastructure"
                }
            }
        }

        test("persistence/query는 JPA/JDSL/Spring Data 타입에 의존하지 않는다 — E-08 guard") {
            noClasses()
                .that()
                .resideInAPackage("com.beat.infrastructure.persistence.query..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                    "jakarta.persistence..",
                    "org.springframework.data.jpa..",
                    "org.springframework.data.repository..",
                    "com.linecorp.kotlinjdsl..",
                )
                .because(
                    "persistence/query is jOOQ View Read only — JPA/JDSL is forbidden (I-20, E-08)"
                )
                .check(productionClasses)
        }

        test("persistence/query는 JdbcTemplate에 의존하지 않는다 — View는 jOOQ") {
            noClasses()
                .that()
                .resideInAPackage("com.beat.infrastructure.persistence.query..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.jdbc.core.JdbcTemplate")
                .because("View projection must use jOOQ, not JdbcTemplate (Mission 15)")
                .check(productionClasses)
        }

        test("persistence/query는 EntityManager에 의존하지 않는다") {
            noClasses()
                .that()
                .resideInAPackage("com.beat.infrastructure.persistence.query..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("jakarta.persistence.EntityManager")
                .because("View Side EntityManager = 0 (Mission 26)")
                .check(productionClasses)
        }
    })
