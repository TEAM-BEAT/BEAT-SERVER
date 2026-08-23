package com.beat.support.security.authentication.internal

import com.beat.support.security.CurrentMember
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * `@CurrentMember Long` / `@CurrentMember long` 파라미터를 인증 principal의 memberId로 변환한다.
 *
 * 미인증 요청은 nullable(`Long`) 파라미터에 `null`을 주입하고,
 * non-null(`long`) 파라미터에는 예외를 던져 계약 위반을 조기에 드러낸다.
 */
internal class CurrentMemberArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentMember::class.java) && parameter.isMemberIdType()

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val authentication = SecurityContextHolder.getContext().authentication
            ?.takeIf { it.isAuthenticated && it !is AnonymousAuthenticationToken }
            ?: return parameter.missingMemberId()

        return authentication.memberId()
    }

    private fun Authentication.memberId(): Long =
        principal as? Long ?: throw IllegalStateException("Current member principal must be a Long")

    private fun MethodParameter.missingMemberId(): Long? {
        if (isPrimitiveLong()) {
            throw IllegalStateException("A non-null @CurrentMember parameter requires authentication")
        }
        return null
    }

    private fun MethodParameter.isMemberIdType(): Boolean =
        parameterType == Long::class.javaObjectType || isPrimitiveLong()

    private fun MethodParameter.isPrimitiveLong(): Boolean = parameterType == Long::class.javaPrimitiveType
}
