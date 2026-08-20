package com.beat.support.security.authentication.internal

import com.beat.support.security.CurrentMember
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.NativeWebRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

class CurrentMemberArgumentResolverTest {

    private val resolver = CurrentMemberArgumentResolver()
    private val webRequest = mock(NativeWebRequest::class.java)

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `boxed Long과 primitive long memberId 파라미터만 지원한다`() {
        assertAll(
            { assertTrue(resolver.supportsParameter(parameter(BOXED))) },
            { assertTrue(resolver.supportsParameter(parameter(PRIMITIVE))) },
            { assertFalse(resolver.supportsParameter(parameter(UNSUPPORTED))) },
        )
    }

    @Test
    fun `인증된 요청은 primitive long 파라미터에 memberId를 주입한다`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(2L, null, AuthorityUtils.createAuthorityList("ROLE_MEMBER"))

        assertEquals(2L, resolver.resolveArgument(parameter(PRIMITIVE), null, webRequest, null))
    }

    @Test
    fun `미인증 요청은 nullable 파라미터에 null을 주입한다`() {
        assertNull(resolver.resolveArgument(parameter(BOXED), null, webRequest, null))
    }

    @Test
    fun `익명 인증은 nullable 파라미터에 null을 주입한다`() {
        SecurityContextHolder.getContext().authentication = AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"),
        )

        assertNull(resolver.resolveArgument(parameter(BOXED), null, webRequest, null))
    }

    @Test
    fun `미인증 요청이 non-null 파라미터를 요구하면 예외를 던진다`() {
        assertThrows<IllegalStateException> {
            resolver.resolveArgument(parameter(PRIMITIVE), null, webRequest, null)
        }
    }

    @Test
    fun `principal이 Long이 아니면 예외를 던진다`() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            "member-name",
            null,
            AuthorityUtils.createAuthorityList("ROLE_MEMBER"),
        )

        assertThrows<IllegalStateException> {
            resolver.resolveArgument(parameter(BOXED), null, webRequest, null)
        }
    }

    private fun parameter(methodName: String): MethodParameter {
        val parameterType = when (methodName) {
            PRIMITIVE -> Long::class.javaPrimitiveType!!
            UNSUPPORTED -> String::class.java
            else -> Long::class.javaObjectType
        }
        return MethodParameter(Handler::class.java.getDeclaredMethod(methodName, parameterType), 0)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    private class Handler {

        fun boxedMemberId(@CurrentMember memberId: Long?) = Unit

        fun primitiveMemberId(@CurrentMember memberId: Long) = Unit

        fun unsupportedMemberId(@CurrentMember memberId: String) = Unit
    }

    companion object {
        private const val BOXED = "boxedMemberId"
        private const val PRIMITIVE = "primitiveMemberId"
        private const val UNSUPPORTED = "unsupportedMemberId"
    }
}
