package com.beat.admin.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter

@Configuration(proxyBeanMethods = false)
class AdminSecurityConfig(
    @param:Qualifier("gatewayJwtAuthenticationFilter")
    private val jwtAuthenticationFilter: OncePerRequestFilter,
    @param:Qualifier("gatewaySecurityMdcLoggingFilter")
    private val securityMdcLoggingFilter: OncePerRequestFilter,
    private val authenticationEntryPoint: AuthenticationEntryPoint,
    private val accessDeniedHandler: AccessDeniedHandler,
    private val environment: Environment,
    @param:Value("\${management.endpoints.web.base-path}")
    private val actuatorEndPoint: String,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .cors(Customizer.withDefaults())
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
            }
            .authorizeHttpRequests {
                it.requestMatchers(*authWhitelist()).permitAll()
                    .anyRequest().hasAuthority(ROLE_ADMIN)
            }
            .addFilterBefore(securityMdcLoggingFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(jwtAuthenticationFilter, securityMdcLoggingFilter.javaClass)

        return http.build()
    }

    private fun authWhitelist(): Array<String> = buildList {
        addAll(
            listOf(
                "/error",
                "$actuatorEndPoint/health",
                "$actuatorEndPoint/prometheus",
            ),
        )
        if (!environment.acceptsProfiles(Profiles.of("prod"))) addAll(SWAGGER_WHITELIST)
    }.toTypedArray()

    private companion object {
        const val ROLE_ADMIN = "ROLE_ADMIN"
        val SWAGGER_WHITELIST = listOf(
            "/api/admin/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
        )
    }
}