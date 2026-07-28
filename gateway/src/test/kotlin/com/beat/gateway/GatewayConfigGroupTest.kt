package com.beat.gateway

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse

/**
 * gateway bootstrap 계약(공개 표면 → internal config 매핑)을 고정한다.
 * config group이 소유하는 bean 집합은 source 검사로 회귀를 막는다.
 */
class GatewayConfigGroupTest {

    @Test
    fun `config group은 optional stateful security 기능만 노출한다`() {
        assertEquals(
            listOf(GatewayConfigGroup.REFRESH_TOKEN_STORE, GatewayConfigGroup.GUEST_ACCESS),
            GatewayConfigGroup.entries,
        )
    }

    @Test
    fun `GUEST_ACCESS group이 guest 세션·비밀번호·스로틀 서비스를 소유한다`() {
        assertEquals(
            "com.beat.gateway.guest.internal.config.GuestAccessConfig",
            GatewayConfigGroup.GUEST_ACCESS.configClass.name,
        )

        val source = source("guest/internal/config/GuestAccessConfig.kt")

        assertAll(
            { assertTrue(source.contains("RedisConfig::class")) },
            { assertTrue(source.contains("GuestSessionService::class")) },
            { assertTrue(source.contains("GuestPasswordHashService::class")) },
            { assertTrue(source.contains("GuestAccessThrottleService::class")) },
        )
    }

    @Test
    fun `servlet security 공개 annotation이 실행 모듈의 static import 표면이다`() {
        val annotationSource = source("EnableGatewayServletSecurity.kt")
        val configSource = source("authentication/internal/config/ServletSecurityConfig.kt")

        assertAll(
            { assertTrue(annotationSource.contains("@Import(ServletSecurityConfig::class)")) },
            { assertTrue(configSource.contains("JwtConfig::class")) },
            { assertTrue(configSource.contains("SecurityFilterConfig::class")) },
            { assertTrue(configSource.contains("WebMvcConfig::class")) },
            { assertFalse(configSource.contains("RefreshTokenConfig::class")) },
            { assertFalse(configSource.contains("RedisConfig::class")) },
            { assertFalse(configSource.contains("RefreshTokenService::class")) },
        )
    }

    @Test
    fun `MDC filter는 servlet security group에 속하고 servlet 자동 등록은 비활성화된다`() {
        val source = source("authentication/internal/config/SecurityFilterConfig.kt")

        assertAll(
            { assertTrue(source.contains("gatewaySecurityMdcLoggingFilter")) },
            { assertTrue(source.contains("FilterRegistrationBean<SecurityMdcLoggingFilter>")) },
            { assertTrue(source.contains("registration.isEnabled = false")) },
        )
    }

    @Test
    fun `REFRESH_TOKEN_STORE group이 Redis repository와 refresh token 서비스를 소유한다`() {
        assertEquals(
            "com.beat.gateway.refreshtoken.internal.config.RefreshTokenConfig",
            GatewayConfigGroup.REFRESH_TOKEN_STORE.configClass.name,
        )

        val source = source("refreshtoken/internal/config/RefreshTokenConfig.kt")

        assertAll(
            { assertTrue(source.contains("RedisConfig::class")) },
            { assertTrue(source.contains("RefreshTokenService::class")) },
        )
    }

    @Test
    fun `JWT filter가 이미 초기화된 MDC에 인증 사용자 id를 채운다`() {
        val source = source("authentication/internal/JwtAuthenticationFilter.kt")

        assertTrue(source.contains("MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, memberId.toString())"))
    }

    @Test
    fun `JwtConfig는 JWT 관련 bean만 등록하고 refresh token·Redis를 가져오지 않는다`() {
        val source = source("jwt/internal/config/JwtConfig.kt")

        assertAll(
            { assertTrue(source.contains("fun jwtTokenProvider(")) },
            { assertTrue(source.contains("JwtTokenProvider")) },
            { assertFalse(source.contains("RefreshTokenService")) },
            { assertFalse(source.contains("RedisConfig")) },
        )
    }

    private fun source(relativePath: String): String =
        Files.readString(Path.of("src/main/kotlin/com/beat/gateway/$relativePath"))
}
