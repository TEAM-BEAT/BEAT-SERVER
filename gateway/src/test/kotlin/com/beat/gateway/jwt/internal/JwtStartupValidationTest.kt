package com.beat.gateway.jwt.internal

import io.jsonwebtoken.security.WeakKeyException
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * 기동 시점 fail-fast 계약을 고정한다.
 *
 * - 값의 존재/범위: [JwtProperties]의 Bean Validation (Spring Boot 권장 방식)
 * - 값으로 실제 서명 키를 만들 수 있는지: [JwtSigningKeyHolder.validateSigningKey]
 */
class JwtStartupValidationTest {

    private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `keyId가 비어 있으면 프로퍼티 검증에서 실패한다`() {
        val violations = validator.validate(properties(keyId = ""))

        assertEquals(1, violations.size)
        assertEquals("keyId", violations.first().propertyPath.toString())
    }

    @Test
    fun `secret이 비어 있으면 프로퍼티 검증에서 실패한다`() {
        val violations = validator.validate(properties(secret = " "))

        assertEquals(1, violations.size)
        assertEquals("secret", violations.first().propertyPath.toString())
    }

    @Test
    fun `access token 만료 시간이 양수가 아니면 프로퍼티 검증에서 실패한다`() {
        val violations = validator.validate(properties(accessTokenExpireTime = 0L))

        assertEquals(1, violations.size)
        assertEquals("accessTokenExpireTime", violations.first().propertyPath.toString())
    }

    @Test
    fun `refresh token 만료 시간이 양수가 아니면 프로퍼티 검증에서 실패한다`() {
        val violations = validator.validate(properties(refreshTokenExpireTime = -1L))

        assertEquals(1, violations.size)
        assertEquals("refreshTokenExpireTime", violations.first().propertyPath.toString())
    }

    @Test
    fun `유효한 프로퍼티는 검증을 통과한다`() {
        assertTrue(validator.validate(properties()).isEmpty())
    }

    @Test
    fun `Base64가 아닌 secret은 기동 시점에 서명 키 생성으로 검출된다`() {
        val holder = JwtSigningKeyHolder(properties(secret = "not-base64!!"))

        assertThrows<RuntimeException> { holder.validateSigningKey() }
    }

    @Test
    fun `HS256 최소 강도 미달 secret은 기동 시점에 검출된다`() {
        val weakSecret = Base64.getEncoder().encodeToString("weak".toByteArray())
        val holder = JwtSigningKeyHolder(properties(secret = weakSecret))

        assertThrows<WeakKeyException> { holder.validateSigningKey() }
    }

    @Test
    fun `서명 키는 한 번만 생성되어 재사용된다`() {
        val holder = JwtSigningKeyHolder(properties())

        assertTrue(holder.signingKey === holder.signingKey)
    }

    private fun properties(
        secret: String = STRONG_BASE64_SECRET,
        accessTokenExpireTime: Long = 3_600_000L,
        refreshTokenExpireTime: Long = 1_209_600_000L,
        keyId: String = "test-current",
    ) = JwtProperties(secret, accessTokenExpireTime, refreshTokenExpireTime, keyId)

    companion object {
        private const val STRONG_BASE64_SECRET =
            "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyAhIiMkJSYnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4/QA=="
    }
}
