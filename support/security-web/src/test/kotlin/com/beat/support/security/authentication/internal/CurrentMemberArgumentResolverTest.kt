package com.beat.support.security.authentication.internal

import com.beat.support.security.CurrentMember
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.NativeWebRequest

class CurrentMemberArgumentResolverTest : FunSpec() {

    private val resolver = CurrentMemberArgumentResolver()
    private val webRequest = mockk<NativeWebRequest>(relaxed = true)

    init {
        isolationMode = IsolationMode.SingleInstance

        afterEach { clearSecurityContext() }

        test("boxed Long과 primitive long memberId 파라미터만 지원한다") {
            resolver.supportsParameter(parameter(BOXED)) shouldBe true
            resolver.supportsParameter(parameter(PRIMITIVE)) shouldBe true
            resolver.supportsParameter(parameter(UNSUPPORTED)) shouldBe false
        }

        test("인증된 요청은 primitive long 파라미터에 memberId를 주입한다") {
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(
                    2L,
                    null,
                    AuthorityUtils.createAuthorityList("ROLE_MEMBER"),
                )

            resolver.resolveArgument(parameter(PRIMITIVE), null, webRequest, null) shouldBe 2L
        }

        test("미인증 요청은 nullable 파라미터에 null을 주입한다") {
            resolver.resolveArgument(parameter(BOXED), null, webRequest, null) shouldBe null
        }

        test("익명 인증은 nullable 파라미터에 null을 주입한다") {
            SecurityContextHolder.getContext().authentication =
                AnonymousAuthenticationToken(
                    "key",
                    "anonymousUser",
                    AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"),
                )

            resolver.resolveArgument(parameter(BOXED), null, webRequest, null) shouldBe null
        }

        test("미인증 요청이 non-null 파라미터를 요구하면 예외를 던진다") {
            shouldThrow<IllegalStateException> {
                resolver.resolveArgument(parameter(PRIMITIVE), null, webRequest, null)
            }
        }

        test("principal이 Long이 아니면 예외를 던진다") {
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(
                    "member-name",
                    null,
                    AuthorityUtils.createAuthorityList("ROLE_MEMBER"),
                )

            shouldThrow<IllegalStateException> {
                resolver.resolveArgument(parameter(BOXED), null, webRequest, null)
            }
        }
    }

    private fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    private fun parameter(methodName: String): MethodParameter {
        val parameterType =
            when (methodName) {
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
