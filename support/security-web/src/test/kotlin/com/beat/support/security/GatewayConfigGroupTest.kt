package com.beat.support.security

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.context.annotation.Import

/** gateway bootstrap 계약(공개 표면 → compiled config metadata 매핑)을 고정한다. */
class GatewayConfigGroupTest : FunSpec() {

    init {
        isolationMode = IsolationMode.SingleInstance

        test("config group은 gateway가 소유하는 optional security 기능만 노출한다") {
            GatewayConfigGroup.entries shouldBe listOf(GatewayConfigGroup.GUEST_ACCESS)
        }

        test("GUEST_ACCESS group은 password hasher configuration만 import한다") {
            val guestAccessConfig = GatewayConfigGroup.GUEST_ACCESS.configClass

            guestAccessConfig.name shouldBe
                "com.beat.support.security.guest.internal.config.GuestAccessConfig"
            guestAccessConfig.importedClassSimpleNames() shouldBe setOf("BCryptPasswordHasher")
        }

        test("servlet security 공개 annotation이 compiled static import 표면이다") {
            val servletSecurityConfig =
                EnableGatewayServletSecurity::class
                    .java
                    .getAnnotation(Import::class.java)
                    .value
                    .single()
                    .java

            servletSecurityConfig.name shouldBe
                "com.beat.support.security.authentication.internal.config.ServletSecurityConfig"
            servletSecurityConfig.importedClassSimpleNames() shouldBe
                setOf("JwtConfig", "SecurityFilterConfig", "WebMvcConfig")
        }
    }

    private fun Class<*>.importedClassSimpleNames(): Set<String> =
        getAnnotation(Import::class.java)?.value?.map { it.java.simpleName }?.toSet() ?: emptySet()
}
