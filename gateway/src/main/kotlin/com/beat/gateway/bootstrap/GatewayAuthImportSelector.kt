package com.beat.gateway.bootstrap

import org.springframework.context.annotation.DeferredImportSelector
import org.springframework.core.type.AnnotationMetadata

/**
 * Gateway가 소유하는 auth/security bean을 FQCN 문자열로 import 한다.
 *
 * gateway 모듈은 root project에 compile 의존하지 않으므로
 * class reference 대신 FQCN string으로 deferred import 한다.
 * auth 코드가 gateway로 물리 이동하면 이 selector는 제거된다.
 */
class GatewayAuthImportSelector : DeferredImportSelector {

    override fun selectImports(importingClassMetadata: AnnotationMetadata): Array<String> =
        AUTH_BEANS + CONFIG_BEANS

    companion object {
        /** auth 패키지 소속 bean (filter, provider, resolver, security handler) */
        val AUTH_BEANS = arrayOf(
            "com.beat.global.auth.jwt.filter.JwtAuthenticationFilter",
            "com.beat.global.auth.jwt.provider.JwtTokenProvider",
            "com.beat.global.auth.resolver.CurrentMemberArgumentResolver",
            "com.beat.global.auth.security.CustomAccessDeniedHandler",
            "com.beat.global.auth.security.CustomJwtAuthenticationEntryPoint",
        )

        /** SecurityConfig, WebConfig — auth 패키지 밖이지만 auth 빈에 의존 */
        val CONFIG_BEANS = arrayOf(
            "com.beat.global.common.config.SecurityConfig",
            "com.beat.global.common.config.WebConfig",
        )
    }
}
