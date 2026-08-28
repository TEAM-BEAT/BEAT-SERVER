package com.beat.support.security.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.FunSpec
import java.nio.file.Files
import java.nio.file.Path

/**
 * Kotlin `internal` only blocks cross-module access; inside this module the public technical APIs
 * could legally reach internals. This suite guards that boundary.
 */
class SupportSecurityArchitectureTest :
    FunSpec({
        val productionClasses: JavaClasses by lazy {
            val productionClassPaths =
                listOf(
                        Path.of("build/classes/kotlin/main"),
                        Path.of("build/classes/java/main"),
                    )
                    .filter(Files::exists)
            require(productionClassPaths.isNotEmpty()) {
                "Support security production class output is missing"
            }
            ClassFileImporter().importPaths(productionClassPaths)
        }

        test("공개 기술 API는 internal 구현에 의존하지 않는다") {
            // Token/Password Port는 application:frontoffice/security로 이동 — support:security에는 토큰
            // 패키지가 없음
            // ArchUnit allowEmptyShould로 빈 패키지 검증을 허용
            noClasses()
                .that()
                .resideInAnyPackage("com.beat.support.security.token..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.beat.support.security..internal..")
                .because(
                    "Token technical APIs must not expose or depend on internal implementations"
                )
                .allowEmptyShould(true)
                .check(productionClasses)

            noClasses()
                .that()
                .haveFullyQualifiedName("com.beat.support.security.password.PasswordHasher")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.beat.support.security..internal..")
                .because("PasswordHasher must not expose or depend on internal implementations")
                .allowEmptyShould(true)
                .check(productionClasses)
        }
    })
