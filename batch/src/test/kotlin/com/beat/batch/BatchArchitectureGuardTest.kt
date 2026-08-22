package com.beat.batch

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.scheduling.annotation.Scheduled
import java.nio.file.Files
import java.nio.file.Path

class BatchArchitectureGuardTest {

    private val productionClasses: JavaClasses by lazy {
        val productionClassPaths = listOf(
            Path.of("build/classes/kotlin/main"),
            Path.of("build/classes/java/main"),
        ).filter(Files::exists)
        require(productionClassPaths.isNotEmpty()) {
            "Batch production class output is missing"
        }
        ClassFileImporter().importPaths(productionClassPaths)
    }

    @Test
    fun `production classes stay in the batch owner`() {
        classes()
            .should()
            .resideInAnyPackage("com.beat.batch..")
            .because("Batch production classes must remain owned by the batch adapter")
            .check(productionClasses)
    }

    @Test
    fun `legacy owner packages are absent from the compiled batch output`() {
        val violations = productionClasses
            .filter { it.packageName.startsWith("com.beat.domain.") || it.packageName.startsWith("com.beat.global.") }
            .map(JavaClass::getFullName)
            .sorted()

        assertTrue(
            violations.isEmpty(),
            "Batch production output must not reintroduce legacy owner packages: ${violations.joinToString(", ")}",
        )
    }

    @Test
    fun `scheduled entrypoints live in batch job packages`() {
        methods()
            .that()
            .areAnnotatedWith(Scheduled::class.java)
            .should()
            .beDeclaredInClassesThat()
            .resideInAnyPackage("com.beat.batch..job..")
            .because("Scheduled batch entrypoints belong to job packages")
            .check(productionClasses)
    }

    @Test
    fun `batch jobs depend on System application and not Domain or infrastructure implementations`() {
        classes()
            .that()
            .resideInAnyPackage("com.beat.batch..job..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.beat.application.system..")
            .because("Batch jobs must enter use cases through the System application boundary")
            .check(productionClasses)

        noClasses()
            .that()
            .resideInAnyPackage("com.beat.batch..job..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.beat.application.frontoffice..",
                "com.beat.application.admin..",
                "com.beat.domain..",
                "com.beat.infra..",
                "com.beat.support.security..",
            )
            .because("Batch jobs must not bypass the System application boundary")
            .check(productionClasses)
    }

    @Test
    fun `batch classes do not reach root or other runtime lanes`() {
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.beat.BeatApplication",
                "com.beat.legacyroot..",
                "com.beat.global..",
                "com.beat.support.security..",
                "com.beat.apis..",
                "com.beat.admin..",
                "com.beat.application.frontoffice..",
                "com.beat.application.admin..",
            )
            .because("Batch must remain an isolated runtime lane")
            .check(productionClasses)
    }

    @Test
    fun `only batch configuration and bootstrap may depend on infrastructure configuration`() {
        noClasses()
            .that(nonBootstrapClasses)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.beat.infra..")
            .because("Infrastructure configuration is visible only to batch bootstrap configuration")
            .check(productionClasses)
    }

    private val nonBootstrapClasses =
        object : DescribedPredicate<JavaClass>("be a batch class outside configuration/bootstrap") {
            override fun test(input: JavaClass): Boolean {
                if (!input.packageName.startsWith("com.beat.batch.")) {
                    return false
                }
                return !input.packageName.contains(".config") && input.simpleName != "BatchApplication"
            }
        }
}
