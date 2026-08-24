package com.beat.application.frontoffice.architecture

import com.beat.application.frontoffice.query.PresentationReadModel
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.FunSpec
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path

/**
 * Capability → Actor → CQRS semantics inside the frontoffice lane. These rules encode
 * design intent that neither the Gradle graph (single module) nor Kotlin visibility
 * can express: command/query separation, actor lanes, cross-capability isolation,
 * auth collaboration boundaries, and failure translation.
 */
private class PackagePattern(val archUnitPattern: String, val segments: List<String>)

private fun packagePattern(vararg segments: String): PackagePattern =
    PackagePattern(
        archUnitPattern = "..${segments.joinToString(".")}..",
        segments = segments.toList(),
    )

class FrontofficeApplicationArchitectureTest : FunSpec({
    val importedClasses: JavaClasses by lazy {
        val productionClassPaths = listOf(
            Path.of("build/classes/kotlin/main"),
            Path.of("build/classes/java/main"),
        ).filter(Files::exists)
        require(productionClassPaths.isNotEmpty()) {
            "Frontoffice production class output is missing"
        }
        ClassFileImporter().importPaths(productionClassPaths)
    }

    fun hasPackage(pattern: PackagePattern): Boolean =
        importedClasses.any { javaClass ->
            javaClass.packageName
                .split('.')
                .windowed(pattern.segments.size)
                .any { it == pattern.segments }
        }

    fun checkRule(rule: ArchRule?) {
        rule?.check(importedClasses)
    }

    fun noDependencyRule(
        sourcePackage: PackagePattern,
        vararg targetPackages: PackagePattern,
    ): ArchRule? {
        if (!hasPackage(sourcePackage)) {
            return null
        }
        val targets = targetPackages.map { it.archUnitPattern }.toTypedArray()
        return noClasses()
            .that()
            .resideInAnyPackage(sourcePackage.archUnitPattern)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*targets)
            .because(
                "${sourcePackage.archUnitPattern} must not depend on ${targets.joinToString()}",
            )
    }

    fun checkActorAlignment(sourceSegment: String, actorPackage: String) {
        val sourcePrefix = "com.beat.application.frontoffice.$sourceSegment"
        val actorPrefix = "com.beat.application.frontoffice.$actorPackage"
        classes()
            .that()
            .resideInAnyPackage(sourcePrefix, "$sourcePrefix..")
            .should()
            .resideInAnyPackage(actorPrefix, "$actorPrefix..")
            .because("$sourcePrefix classes must reside under $actorPrefix")
            .check(importedClasses)
    }

    fun noDependencyOnConcreteTypesRule(
        sourcePackage: PackagePattern,
        forbiddenTypes: Set<String>,
    ): ArchRule? {
        if (!hasPackage(sourcePackage) || forbiddenTypes.isEmpty()) {
            return null
        }
        val forbiddenTypePredicate = object : DescribedPredicate<JavaClass>(
            "have forbidden concrete types ${forbiddenTypes.sorted().joinToString()}",
        ) {
            override fun test(input: JavaClass): Boolean = input.fullName in forbiddenTypes
        }
        return noClasses()
            .that()
            .resideInAnyPackage(sourcePackage.archUnitPattern)
            .should()
            .dependOnClassesThat(forbiddenTypePredicate)
            .because(
                "${sourcePackage.archUnitPattern} must not depend on forbidden concrete types "
                    + forbiddenTypes.sorted().joinToString(),
            )
    }

    fun noMemberDependencyOnAuthTypesRule(): ArchRule? {
        val allowedAuthTypes = setOf(
            "com.beat.application.frontoffice.auth.command.LoginSessionIssuer",
            "com.beat.application.frontoffice.auth.command.LoginSession",
        )
        val forbiddenAuthTypes = importedClasses
            .filter { javaClass ->
                javaClass.packageName.startsWith("com.beat.application.frontoffice.auth") &&
                    javaClass.fullName !in allowedAuthTypes
            }
            .map(JavaClass::getFullName)
            .toSet()
        return noDependencyOnConcreteTypesRule(packagePattern("member"), forbiddenAuthTypes)
    }

    test("command 패키지는 presentation read-model에 의존하지 않는다") {
        noClasses()
            .that()
            .resideInAnyPackage("..command..")
            .should()
            .dependOnClassesThat()
            .areAnnotatedWith(PresentationReadModel::class.java)
            .because("Command Correctness vs Presentation Read Model")
            .check(importedClasses)
    }

    test("PresentationReadModel annotation은 production type에 적용된다") {
        val annotatedProductionTypes = importedClasses.filter { javaClass ->
            javaClass.isAnnotatedWith(PresentationReadModel::class.java)
        }
        check(annotatedProductionTypes.isNotEmpty()) {
            "PresentationReadModel annotation matches no production types."
        }
    }

    test("query ReadModel은 PresentationReadModel marker를 가진다") {
        classes()
            .that()
            .resideInAnyPackage("..query..")
            .and()
            .areNotAnnotations()
            .and()
            .haveSimpleNameEndingWith("ReadModel")
            .should()
            .beAnnotatedWith(PresentationReadModel::class.java)
            .because("Command Correctness vs Presentation Read Model marker coverage")
            .check(importedClasses)
    }

    test("query Reader interface는 PresentationReadModel marker를 가진다") {
        classes()
            .that()
            .resideInAnyPackage("..query..")
            .and()
            .areInterfaces()
            .and()
            .haveSimpleNameEndingWith("Reader")
            .should()
            .beAnnotatedWith(PresentationReadModel::class.java)
            .because("Command Correctness vs Presentation Read Model marker coverage")
            .check(importedClasses)
    }

    test("query Projection은 PresentationReadModel marker를 가진다") {
        classes()
            .that()
            .resideInAnyPackage("..query..")
            .and()
            .areNotAnnotations()
            .and()
            .haveSimpleNameEndingWith("Projection")
            .should()
            .beAnnotatedWith(PresentationReadModel::class.java)
            .because("Command Correctness vs Presentation Read Model marker coverage")
            .check(importedClasses)
    }

    test("booking booker 패키지는 performance 레인에 의존하지 않는다") {
        checkRule(
            noDependencyRule(
                packagePattern("booking", "booker"),
                packagePattern("performance", "booker"),
                packagePattern("performance", "maker"),
            ),
        )
    }

    test("booking 클래스는 booker 레인 안에만 존재한다") {
        checkActorAlignment("booking", "booking.booker")
    }

    test("ticket 클래스는 maker 레인 안에만 존재한다") {
        checkActorAlignment("ticket", "ticket.maker")
    }

    test("home 클래스는 booker 레인 안에만 존재한다") {
        checkActorAlignment("home", "home.booker")
    }

    test("performance booker query는 maker에 의존하지 않는다") {
        checkRule(
            noDependencyRule(
                packagePattern("performance", "booker", "query"),
                packagePattern("performance", "maker"),
            ),
        )
    }

    test("performance maker query는 booker와 maker command에 의존하지 않는다") {
        checkRule(
            noDependencyRule(
                packagePattern("performance", "maker", "query"),
                packagePattern("performance", "booker"),
                packagePattern("performance", "maker", "command"),
            ),
        )
    }

    test("performance maker command는 booker·maker query와 schedule query에 의존하지 않는다") {
        checkRule(
            noDependencyRule(
                packagePattern("performance", "maker", "command"),
                packagePattern("performance", "booker"),
                packagePattern("performance", "maker", "query"),
                packagePattern("schedule", "booker", "query"),
            ),
        )
    }

    test("auth는 member에 의존하지 않는다") {
        checkRule(
            noDependencyRule(
                packagePattern("auth"),
                packagePattern("member"),
            ),
        )
    }

    test("member는 auth의 LoginSession 경계로만 협업한다") {
        checkRule(noMemberDependencyOnAuthTypesRule())
    }

    test("member는 support token 발급기를 직접 의존하지 않는다") {
        checkRule(
            noDependencyOnConcreteTypesRule(
                packagePattern("member"),
                setOf(
                    "com.beat.application.frontoffice.security.TokenIssuer",
                    "com.beat.application.frontoffice.security.RefreshTokenAuthenticator",
                ),
            ),
        )
    }

    test("ApplicationService는 다른 ApplicationService에 직접 의존하지 않는다") {
        noClasses()
            .that()
            .areAnnotatedWith(Service::class.java)
            .should()
            .dependOnClassesThat()
            .areAnnotatedWith(Service::class.java)
            .because("No Application Service Graph: use-case entry points must not form a concrete service graph")
            .check(importedClasses)
    }

    test("frontoffice Application은 기술 구현에 의존하지 않는다") {
        noClasses()
            .that()
            .resideInAnyPackage("com.beat.application.frontoffice", "com.beat.application.frontoffice..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "jakarta.persistence..",
                "org.springframework.web..",
                "org.springframework.data.redis..",
                "org.redisson..",
                "com.linecorp.kotlinjdsl..",
            )
            .because("Frontoffice Application technology boundary")
            .check(importedClasses)
    }

    test("Component implementation은 @Transactional 메서드를 선언하지 않는다") {
        methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Component::class.java)
            .and()
            .areDeclaredInClassesThat()
            .areNotAnnotatedWith(Service::class.java)
            .should()
            .notBeAnnotatedWith(Transactional::class.java)
            .because("Component implementation boundaries must not own transactions")
            .check(importedClasses)
    }
})
