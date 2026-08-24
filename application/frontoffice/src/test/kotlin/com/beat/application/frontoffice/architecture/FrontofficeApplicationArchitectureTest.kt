package com.beat.application.frontoffice.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.FunSpec
import org.springframework.stereotype.Service

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
    val importedClasses: JavaClasses =
        ClassFileImporter().importPackages("com.beat.application.frontoffice")

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

    fun noCommandDependencyOnPresentationReadModelsRule(): ArchRule? {
        val sourcePackage = packagePattern("command")
        if (!hasPackage(sourcePackage)) {
            return null
        }
        val documentedExceptions = setOf(
            // FINAL-REPORT §8: primary-DB 403/404 diagnostic reader for Performance modify commands.
            "com.beat.application.frontoffice.performance.maker.command.PerformanceContentOwnershipReader",
        )
        val presentationReadModel = object : DescribedPredicate<JavaClass>(
            "be a presentation read-model type owned by the query side",
        ) {
            override fun test(input: JavaClass): Boolean {
                if (input.fullName in documentedExceptions) {
                    return false
                }
                if (!input.packageName.startsWith("com.beat.")) {
                    return false
                }
                val name = input.simpleName
                return input.packageName.split('.').contains("readmodel") ||
                    name.endsWith("Reader") ||
                    name.endsWith("Queries") ||
                    name.endsWith("ReadModel") ||
                    name.endsWith("Projection")
            }
        }
        return noClasses()
            .that()
            .resideInAnyPackage(sourcePackage.archUnitPattern)
            .should()
            .dependOnClassesThat(presentationReadModel)
            .because(
                "${sourcePackage.archUnitPattern} correctness code must not depend on presentation projections",
            )
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

    test("command 패키지는 query 패키지에 의존하지 않는다") {
        checkRule(
            noDependencyRule(packagePattern("command"), packagePattern("query")),
        )
    }

    test("read-model 가드는 실제 검사 대상이 존재한다") {
        val matched = importedClasses.filter { javaClass ->
            javaClass.packageName.startsWith("com.beat.") &&
                (
                    javaClass.simpleName.endsWith("Reader") ||
                        javaClass.simpleName.endsWith("Queries") ||
                        javaClass.simpleName.endsWith("ReadModel") ||
                        javaClass.simpleName.endsWith("Projection")
                    )
        }
        check(matched.isNotEmpty()) {
            "Presentation read-model predicate matches nothing; the command-side guard is vacuous."
        }
    }

    test("command 패키지는 presentation read-model에 의존하지 않는다") {
        checkRule(noCommandDependencyOnPresentationReadModelsRule())
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
                    "com.beat.support.security.token.TokenIssuer",
                    "com.beat.support.security.token.RefreshTokenAuthenticator",
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

    test("모든 @Service는 도메인 실패 번역기를 거친다") {
        val failureTranslator = object : DescribedPredicate<JavaClass>(
            "be the domain failure translator",
        ) {
            override fun test(javaClass: JavaClass): Boolean =
                javaClass.fullName == "com.beat.application.frontoffice.exception.DomainFailureTranslatorKt"
        }
        classes()
            .that()
            .areAnnotatedWith(Service::class.java)
            .should()
            .dependOnClassesThat(failureTranslator)
            .because("service use-case boundaries must translate domain failures")
            .check(importedClasses)
    }
})
