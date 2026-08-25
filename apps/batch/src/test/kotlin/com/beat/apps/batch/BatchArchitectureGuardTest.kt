package com.beat.apps.batch

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.FunSpec
import java.nio.file.Files
import java.nio.file.Path
import org.springframework.scheduling.annotation.Scheduled

/**
 * Batch composition-root contracts. Module-level isolation (only :application:system,
 * :infrastructure, :support:observability on the classpath) is enforced by the Gradle graph; these
 * rules cover what remains invisible to the build.
 */
class BatchArchitectureGuardTest :
    FunSpec({
        val productionClasses: JavaClasses by lazy {
            val productionClassPaths =
                listOf(
                        Path.of("build/classes/kotlin/main"),
                        Path.of("build/classes/java/main"),
                    )
                    .filter(Files::exists)
            require(productionClassPaths.isNotEmpty()) {
                "Batch production class output is missing"
            }
            ClassFileImporter().importPaths(productionClassPaths)
        }

        test("@Scheduled 진입점은 batch job 패키지에만 위치한다") {
            methods()
                .that()
                .areAnnotatedWith(Scheduled::class.java)
                .should()
                .beDeclaredInClassesThat()
                .resideInAnyPackage("com.beat.apps.batch..job..")
                .because("Scheduled batch entrypoints belong to job packages")
                .check(productionClasses)
        }

        test("batch job은 System application 경계로만 워크플로우에 진입한다") {
            noClasses()
                .that()
                .resideInAnyPackage("com.beat.apps.batch..job..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                    "com.beat.application.frontoffice..",
                    "com.beat.application.admin..",
                    "com.beat.support.security..",
                    "com.beat.infrastructure..",
                )
                .because(
                    "Batch jobs must reach business workflows exclusively via application:system use cases"
                )
                .check(productionClasses)
        }

        test("infra 공개 설정 타입은 bootstrap만 사용할 수 있다") {
            val nonBootstrapClasses =
                object :
                    DescribedPredicate<JavaClass>(
                        "be a batch class outside configuration/bootstrap"
                    ) {
                    override fun test(input: JavaClass): Boolean {
                        if (!input.packageName.startsWith("com.beat.apps.batch.")) {
                            return false
                        }
                        return !input.packageName.contains(".config") &&
                            input.simpleName != "BatchApplication"
                    }
                }
            noClasses()
                .that(nonBootstrapClasses)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.beat.infrastructure..")
                .because(
                    "Infrastructure wiring types may only be consumed by batch bootstrap configuration"
                )
                .check(productionClasses)
        }

        test("batch는 jOOQ 구현 타입에 의존하지 않는다 — E-08") {
            noClasses()
                .that()
                .resideInAnyPackage("com.beat.apps.batch", "com.beat.apps.batch..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.jooq..", "com.beat.infrastructure.jooq.generated..")
                .because("Apps must not depend on jOOQ — infrastructure internal (E-08, I-21)")
                .check(productionClasses)
        }
    })
