package com.beat.support.security.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class SupportSecurityArchitectureTest {

    private val productionClasses: JavaClasses by lazy {
        val productionClassPaths = listOf(
            Path.of("build/classes/kotlin/main"),
            Path.of("build/classes/java/main"),
        ).filter(Files::exists)
        require(productionClassPaths.isNotEmpty()) {
            "Support security production class output is missing"
        }
        ClassFileImporter().importPaths(productionClassPaths)
    }

    @Test
    fun `production classes stay under support security package`() {
        classes()
            .should()
            .resideInAnyPackage("com.beat.support.security..")
            .because("support security production classes must not reintroduce com.beat.gateway packages")
            .check(productionClasses)
    }

    @Test
    fun `public technical APIs must not depend on internal implementations`() {
        noClasses()
            .that()
            .resideInAnyPackage("com.beat.support.security.token..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.beat.support.security..internal..")
            .because("Token technical APIs must not expose or depend on internal implementations")
            .check(productionClasses)

        noClasses()
            .that()
            .haveFullyQualifiedName("com.beat.support.security.password.PasswordHasher")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.beat.support.security..internal..")
            .because("PasswordHasher must not expose or depend on internal implementations")
            .check(productionClasses)
    }
}
